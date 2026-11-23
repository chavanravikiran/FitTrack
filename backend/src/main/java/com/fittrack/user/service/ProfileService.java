package com.fittrack.user.service;

import com.fittrack.assessment.entity.FitnessGoal;
import com.fittrack.assessment.repository.FitnessGoalRepository;
import com.fittrack.body.entity.WeightRecord;
import com.fittrack.body.repository.WeightRecordRepository;
import com.fittrack.common.exception.BadRequestException;
import com.fittrack.common.exception.ResourceNotFoundException;
import com.fittrack.recommendation.calculator.BodyMetrics;
import com.fittrack.security.CurrentUserService;
import com.fittrack.storage.ImageProcessor;
import com.fittrack.storage.StorageService;
import com.fittrack.user.dto.ProfileDtos.ProfileResponse;
import com.fittrack.user.dto.ProfileDtos.UpdateProfileRequest;
import com.fittrack.user.entity.ThemePreference;
import com.fittrack.user.entity.User;
import com.fittrack.user.entity.UserProfile;
import com.fittrack.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png");
    private static final int PROFILE_PHOTO_MAX_PX = 512;

    private final UserProfileRepository profileRepository;
    private final WeightRecordRepository weightRepository;
    private final FitnessGoalRepository goalRepository;
    private final StorageService storage;
    private final ImageProcessor imageProcessor;
    private final CurrentUserService currentUser;
    private final Clock clock;

    @Transactional(readOnly = true)
    public ProfileResponse getMyProfile() {
        return toResponse(findMine());
    }

    @Transactional
    public ProfileResponse update(UpdateProfileRequest req) {
        UserProfile p = findMine();
        AgePolicy.validate(req.dateOfBirth(), LocalDate.now(clock.withZone(p.zoneId())));
        p.setFullName(req.fullName().trim());
        p.setDateOfBirth(req.dateOfBirth());
        p.setGender(req.gender());
        p.setHeightCm(req.heightCm());
        p.setTimeZone(req.timeZone());
        p.setTheme(req.theme());
        return toResponse(p);
    }

    @Transactional
    public ProfileResponse updateTheme(ThemePreference theme) {
        UserProfile p = findMine();
        p.setTheme(theme);
        return toResponse(p);
    }

    @Transactional
    public ProfileResponse uploadPhoto(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Please choose an image to upload");
        }
        if (file.getContentType() == null || !ALLOWED_TYPES.contains(file.getContentType())) {
            throw new BadRequestException("Only JPEG and PNG images are supported");
        }
        byte[] jpeg;
        try {
            jpeg = imageProcessor.toJpeg(file.getBytes(), PROFILE_PHOTO_MAX_PX);
        } catch (IOException e) {
            throw new BadRequestException("Could not read the uploaded file");
        }

        UserProfile p = findMine();
        String oldKey = p.getPhotoPath();
        String newKey = storage.store("profile-photos/" + p.getUser().getId(), jpeg, "jpg");
        p.setPhotoPath(newKey);
        // Remove the old file only once the DB change is committed; drop the new one if it rolls back.
        afterCompletion(oldKey, newKey);
        return toResponse(p);
    }

    @Transactional(readOnly = true)
    public byte[] getPhoto() {
        UserProfile p = findMine();
        if (p.getPhotoPath() == null) {
            throw new ResourceNotFoundException("No profile photo");
        }
        return storage.load(p.getPhotoPath());
    }

    @Transactional
    public void deletePhoto() {
        UserProfile p = findMine();
        String key = p.getPhotoPath();
        p.setPhotoPath(null);
        afterCompletion(key, null);
    }

    private void afterCompletion(String deleteOnCommit, String deleteOnRollback) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_COMMITTED && deleteOnCommit != null) {
                    storage.delete(deleteOnCommit);
                } else if (status == STATUS_ROLLED_BACK && deleteOnRollback != null) {
                    storage.delete(deleteOnRollback);
                }
            }
        });
    }

    private UserProfile findMine() {
        return profileRepository.findByUserId(currentUser.userId())
                .orElseThrow(() -> ResourceNotFoundException.of("Profile", currentUser.userId()));
    }

    private ProfileResponse toResponse(UserProfile p) {
        User u = p.getUser();
        Long userId = u.getId();
        BigDecimal currentWeight = weightRepository.findFirstByUserIdOrderByRecordDateDesc(userId)
                .map(WeightRecord::getWeightKg).orElse(null);
        BigDecimal targetWeight = goalRepository.findByUserIdAndGoalTypeAndStatus(
                        userId, FitnessGoal.GoalType.TARGET_WEIGHT, FitnessGoal.GoalStatus.ACTIVE)
                .map(FitnessGoal::getTargetValue).orElse(null);
        BigDecimal bmi = currentWeight != null && p.getHeightCm() != null
                ? BodyMetrics.bmi(currentWeight, p.getHeightCm()) : null;
        LocalDate today = LocalDate.now(clock.withZone(p.zoneId()));

        return new ProfileResponse(userId, u.getEmail(), p.getFullName(), p.getDateOfBirth(), p.ageOn(today),
                p.getGender(), p.getHeightCm(), p.getTimeZone(), p.getTheme(), p.getPhotoPath() != null,
                p.isOnboardingCompleted(),
                u.getRoles().stream().map(r -> r.getName().name()).sorted().toList(),
                currentWeight, targetWeight, bmi, u.getCreatedAt());
    }
}
