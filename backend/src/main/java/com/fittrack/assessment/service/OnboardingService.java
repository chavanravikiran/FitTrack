package com.fittrack.assessment.service;

import com.fittrack.assessment.dto.AssessmentDtos.AssessmentRequest;
import com.fittrack.assessment.dto.AssessmentDtos.OnboardingRequest;
import com.fittrack.assessment.entity.AssessmentEnums.PrimaryGoal;
import com.fittrack.assessment.entity.FitnessGoal.GoalType;
import com.fittrack.body.entity.WeightRecord;
import com.fittrack.body.repository.WeightRecordRepository;
import com.fittrack.common.exception.BusinessRuleException;
import com.fittrack.common.exception.ResourceNotFoundException;
import com.fittrack.security.CurrentUserService;
import com.fittrack.user.dto.ProfileDtos.ProfileResponse;
import com.fittrack.user.entity.User;
import com.fittrack.user.entity.UserProfile;
import com.fittrack.user.repository.UserProfileRepository;
import com.fittrack.user.service.AgePolicy;
import com.fittrack.user.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Persists the onboarding wizard in one transaction: profile details, first assessment,
 * starting weight and target-weight goal.
 */
@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final UserProfileRepository profileRepository;
    private final WeightRecordRepository weightRepository;
    private final AssessmentService assessmentService;
    private final GoalService goalService;
    private final ProfileService profileService;
    private final CurrentUserService currentUser;
    private final Clock clock;

    @Transactional
    public ProfileResponse complete(OnboardingRequest req) {
        UserProfile profile = profileRepository.findByUserId(currentUser.userId())
                .orElseThrow(() -> ResourceNotFoundException.of("Profile", currentUser.userId()));
        User user = profile.getUser();

        LocalDate today = LocalDate.now(clock.withZone(java.time.ZoneId.of(req.timeZone())));
        AgePolicy.validate(req.dateOfBirth(), today);
        validateGoalConsistency(req.assessment(), req);

        profile.setFullName(req.fullName().trim());
        profile.setDateOfBirth(req.dateOfBirth());
        profile.setGender(req.gender());
        profile.setHeightCm(req.heightCm());
        profile.setTimeZone(req.timeZone());
        profile.setOnboardingCompleted(true);
        profileRepository.saveAndFlush(profile);

        assessmentService.createVersion(user, req.assessment());
        upsertWeight(user, today, req);
        goalService.createGoal(user, GoalType.TARGET_WEIGHT, null, req.targetWeightKg(), null);

        return profileService.getMyProfile();
    }

    private void upsertWeight(User user, LocalDate today, OnboardingRequest req) {
        WeightRecord w = weightRepository.findByUserIdAndRecordDate(user.getId(), today)
                .orElseGet(WeightRecord::new);
        w.setUser(user);
        w.setRecordDate(today);
        w.setWeightKg(req.currentWeightKg());
        weightRepository.saveAndFlush(w);
    }

    private static void validateGoalConsistency(AssessmentRequest a, OnboardingRequest req) {
        int cmp = req.targetWeightKg().compareTo(req.currentWeightKg());
        if (a.primaryGoal() == PrimaryGoal.WEIGHT_LOSS && cmp >= 0) {
            throw new BusinessRuleException("For a weight-loss goal, the target weight should be lower than your current weight");
        }
        if (a.primaryGoal() == PrimaryGoal.MUSCLE_GAIN && cmp < 0
                && req.currentWeightKg().subtract(req.targetWeightKg()).doubleValue() > 5) {
            throw new BusinessRuleException("Muscle gain usually means maintaining or slightly increasing weight. "
                    + "Consider 'Weight loss' or 'General fitness' as your primary goal.");
        }
    }
}
