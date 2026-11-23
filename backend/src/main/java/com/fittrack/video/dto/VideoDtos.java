package com.fittrack.video.dto;

import com.fittrack.exercise.dto.ExerciseDtos.MuscleGroupDto;
import com.fittrack.exercise.entity.Exercise.Difficulty;
import com.fittrack.video.entity.ExerciseVideo.Category;
import com.fittrack.video.entity.ExerciseVideo.Provider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class VideoDtos {

    private VideoDtos() {
    }

    public record VideoDto(Long id, String title, Category category, Difficulty difficulty, MuscleGroupDto muscleGroup,
                           Long exerciseId, String exerciseName, List<String> equipment, String instructions,
                           Provider provider, boolean playable, String playUrl, String thumbnailUrl,
                           String licenseNote, boolean active) {
    }

    /**
     * Create/update a library entry. {@code videoUrl} accepts a YouTube or Vimeo link (any common format);
     * leave it empty to keep instructions only or when uploading a file to S3.
     */
    public record VideoRequest(
            @NotBlank @Size(max = 150) String title,
            @NotNull Category category,
            @NotNull Difficulty difficulty,
            Long exerciseId,
            @Size(max = 20) String muscleGroupCode,
            @Size(max = 500) String videoUrl,
            @Size(max = 10_000) String instructions,
            @Size(max = 255) String licenseNote,
            @Size(max = 13) List<@NotBlank String> equipmentCodes,
            boolean active) {
    }

    public record UploadRequest(
            @NotBlank @Pattern(regexp = "video/mp4|video/webm", message = "only MP4 or WebM videos") String contentType,
            @NotNull Long sizeBytes) {
    }

    public record UploadTargetDto(String key, String uploadUrl, String method, String contentType, long maxBytes) {
    }

    public record ConfirmUploadRequest(@NotBlank @Size(max = 300) String key) {
    }
}
