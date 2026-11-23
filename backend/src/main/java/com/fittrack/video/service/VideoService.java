package com.fittrack.video.service;

import com.fittrack.assessment.entity.Equipment;
import com.fittrack.assessment.repository.EquipmentRepository;
import com.fittrack.common.exception.BadRequestException;
import com.fittrack.common.exception.ResourceNotFoundException;
import com.fittrack.common.exception.ServiceUnavailableException;
import com.fittrack.config.AppProperties;
import com.fittrack.exercise.entity.Exercise;
import com.fittrack.exercise.entity.Exercise.Difficulty;
import com.fittrack.exercise.repository.ExerciseRepository;
import com.fittrack.exercise.repository.MuscleGroupRepository;
import com.fittrack.exercise.service.ExerciseService;
import com.fittrack.storage.ImageProcessor;
import com.fittrack.storage.MediaUrlService;
import com.fittrack.storage.StorageKeys;
import com.fittrack.storage.StorageService;
import com.fittrack.video.dto.VideoDtos.UploadTargetDto;
import com.fittrack.video.dto.VideoDtos.VideoDto;
import com.fittrack.video.dto.VideoDtos.VideoRequest;
import com.fittrack.video.entity.ExerciseVideo;
import com.fittrack.video.entity.ExerciseVideo.Category;
import com.fittrack.video.entity.ExerciseVideo.Provider;
import com.fittrack.video.repository.ExerciseVideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VideoService {

    private static final String THUMB_FOLDER = StorageKeys.CATALOG_PREFIX + "thumbnails";
    private static final String VIDEO_FOLDER = StorageKeys.CATALOG_PREFIX + "videos";

    private final ExerciseVideoRepository videoRepository;
    private final ExerciseRepository exerciseRepository;
    private final MuscleGroupRepository muscleGroupRepository;
    private final EquipmentRepository equipmentRepository;
    private final StorageService storage;
    private final MediaUrlService mediaUrls;
    private final ImageProcessor imageProcessor;
    private final AppProperties props;

    @Transactional(readOnly = true)
    public List<VideoDto> library(Category category, String muscle, Difficulty difficulty, String q) {
        String query = StringUtils.hasText(q) ? q.trim().toLowerCase(Locale.ROOT) : null;
        return videoRepository.findActiveLibrary().stream()
                .filter(v -> category == null || v.getCategory() == category)
                .filter(v -> !StringUtils.hasText(muscle) || (v.getMuscleGroup() != null && v.getMuscleGroup().getCode().equalsIgnoreCase(muscle)))
                .filter(v -> difficulty == null || v.getDifficulty() == difficulty)
                .filter(v -> query == null || v.getTitle().toLowerCase(Locale.ROOT).contains(query))
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public VideoDto get(Long id) {
        return videoRepository.findWithDetailsById(id).filter(ExerciseVideo::isActive).map(this::toDto)
                .orElseThrow(() -> ResourceNotFoundException.of("Video", id));
    }

    // ---------------------------------------------------------------- admin

    @Transactional(readOnly = true)
    public List<VideoDto> adminList() {
        return videoRepository.findAllForAdmin().stream().map(this::toDto).toList();
    }

    @Transactional
    public VideoDto create(VideoRequest req) {
        ExerciseVideo v = new ExerciseVideo();
        apply(v, req);
        return toDto(videoRepository.save(v));
    }

    @Transactional
    public VideoDto update(Long id, VideoRequest req) {
        ExerciseVideo v = find(id);
        apply(v, req);
        return toDto(v);
    }

    @Transactional
    public void delete(Long id) {
        ExerciseVideo v = find(id);
        String video = v.getProvider() == Provider.SELF_HOSTED ? v.getEmbedUrl() : null;
        String thumb = v.getThumbnailUrl();
        videoRepository.delete(v);
        deleteAfterCommit(video, thumb);
    }

    /** Thumbnail upload (JPEG/PNG, re-encoded and resized). Stored under catalog/thumbnails in S3 or locally. */
    @Transactional
    public VideoDto uploadThumbnail(Long id, MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BadRequestException("Please choose an image");
        if (file.getSize() > 5L * 1024 * 1024) throw new BadRequestException("Thumbnail must be 5 MB or smaller");
        ExerciseVideo v = find(id);
        byte[] jpeg;
        try {
            jpeg = imageProcessor.toJpeg(file.getBytes(), 960);
        } catch (IOException e) {
            throw new BadRequestException("Could not read the uploaded file");
        }
        String old = v.getThumbnailUrl();
        v.setThumbnailUrl(storage.store(THUMB_FOLDER, jpeg, "jpg", "image/jpeg"));
        deleteAfterCommit(old, null);
        return toDto(v);
    }

    /**
     * Step 1 of a video upload: a presigned S3 PUT URL so the browser uploads the file directly to S3
     * (large files never pass through the API). Requires S3 storage.
     */
    @Transactional(readOnly = true)
    public UploadTargetDto videoUploadTarget(Long id, String contentType, long sizeBytes) {
        find(id);
        long max = props.getStorage().getMaxVideoBytes();
        if (sizeBytes <= 0 || sizeBytes > max) {
            throw new BadRequestException("Video must be smaller than " + (max / (1024 * 1024)) + " MB");
        }
        String ext = "video/webm".equals(contentType) ? "webm" : "mp4";
        var target = storage.uploadTarget(VIDEO_FOLDER, ext, contentType, props.getStorage().getPresignTtl())
                .orElseThrow(() -> new ServiceUnavailableException(
                        "Video uploads need S3 storage (STORAGE_TYPE=S3). You can paste a YouTube or Vimeo link instead."));
        return new UploadTargetDto(target.key(), target.url(), target.method(), target.contentType(), max);
    }

    /** Step 2: after the browser finished uploading, attach the stored file to the entry. */
    @Transactional
    public VideoDto confirmVideoUpload(Long id, String key) {
        ExerciseVideo v = find(id);
        if (!StorageKeys.isCatalog(key) || !key.startsWith(VIDEO_FOLDER + "/")) {
            throw new BadRequestException("Invalid upload key");
        }
        if (!storage.exists(key)) {
            throw new BadRequestException("The upload hasn't finished yet - please try again in a moment.");
        }
        String old = v.getProvider() == Provider.SELF_HOSTED ? v.getEmbedUrl() : null;
        v.setProvider(Provider.SELF_HOSTED);
        v.setEmbedUrl(key);
        deleteAfterCommit(old, null);
        return toDto(v);
    }

    // ---------------------------------------------------------------- helpers

    private void apply(ExerciseVideo v, VideoRequest req) {
        v.setTitle(req.title().trim());
        v.setCategory(req.category());
        v.setDifficulty(req.difficulty());
        v.setInstructions(StringUtils.hasText(req.instructions()) ? req.instructions().trim() : null);
        v.setLicenseNote(StringUtils.hasText(req.licenseNote()) ? req.licenseNote().trim() : null);
        v.setActive(req.active());

        Exercise ex = req.exerciseId() == null ? null : exerciseRepository.findById(req.exerciseId())
                .orElseThrow(() -> ResourceNotFoundException.of("Exercise", req.exerciseId()));
        v.setExercise(ex);
        if (StringUtils.hasText(req.muscleGroupCode())) {
            v.setMuscleGroup(muscleGroupRepository.findByCode(req.muscleGroupCode().toUpperCase(Locale.ROOT))
                    .orElseThrow(() -> new BadRequestException("Unknown muscle group: " + req.muscleGroupCode())));
        } else {
            v.setMuscleGroup(ex == null ? null : ex.getPrimaryMuscleGroup());
        }

        if (StringUtils.hasText(req.videoUrl())) {
            var parsed = VideoLinks.parse(req.videoUrl())
                    .orElseThrow(() -> new BadRequestException("Paste a YouTube or Vimeo link, or upload a video file."));
            if (v.getProvider() == Provider.SELF_HOSTED) {
                deleteAfterCommit(v.getEmbedUrl(), null);
            }
            v.setProvider(parsed.provider());
            v.setEmbedUrl(parsed.embedUrl());
        } else if (v.getProvider() != Provider.SELF_HOSTED) {
            v.setEmbedUrl(null);
            v.setProvider(Provider.YOUTUBE);
        }

        Set<String> codes = req.equipmentCodes() == null ? Set.of() : new HashSet<>(req.equipmentCodes());
        List<Equipment> eq = codes.isEmpty() ? List.of() : equipmentRepository.findByCodeIn(codes);
        if (eq.size() != codes.size()) throw new BadRequestException("Unknown equipment code");
        v.setEquipment(new HashSet<>(eq));
    }

    VideoDto toDto(ExerciseVideo v) {
        String playUrl = null;
        if (StringUtils.hasText(v.getEmbedUrl())) {
            playUrl = v.getProvider() == Provider.SELF_HOSTED ? mediaUrls.catalogUrl(v.getEmbedUrl()) : v.getEmbedUrl();
        }
        String thumb = StringUtils.hasText(v.getThumbnailUrl()) ? mediaUrls.catalogUrl(v.getThumbnailUrl())
                : (v.getProvider() == Provider.YOUTUBE ? VideoLinks.youtubeThumbnail(v.getEmbedUrl()) : null);
        Set<Equipment> equipment = !v.getEquipment().isEmpty() ? v.getEquipment()
                : (v.getExercise() == null ? Set.of() : v.getExercise().getEquipment());
        return new VideoDto(v.getId(), v.getTitle(), v.getCategory(), v.getDifficulty(),
                v.getMuscleGroup() == null ? null : ExerciseService.toDto(v.getMuscleGroup()),
                v.getExercise() == null ? null : v.getExercise().getId(),
                v.getExercise() == null ? null : v.getExercise().getName(),
                equipment.stream().map(Equipment::getCode).filter(c -> !"NONE".equals(c)).sorted().collect(Collectors.toList()),
                v.getInstructions(), v.getProvider(), playUrl != null, playUrl, thumb, v.getLicenseNote(), v.isActive());
    }

    private ExerciseVideo find(Long id) {
        return videoRepository.findWithDetailsById(id).orElseThrow(() -> ResourceNotFoundException.of("Video", id));
    }

    private void deleteAfterCommit(String key1, String key2) {
        if (key1 == null && key2 == null) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                if (key1 != null && StorageKeys.isCatalog(key1)) storage.delete(key1);
                if (key2 != null && StorageKeys.isCatalog(key2)) storage.delete(key2);
            }
        });
    }
}
