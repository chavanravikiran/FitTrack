package com.fittrack.assessment.service;

import com.fittrack.assessment.dto.GoalDtos.GoalRequest;
import com.fittrack.assessment.dto.GoalDtos.GoalResponse;
import com.fittrack.assessment.dto.GoalDtos.GoalUpdateRequest;
import com.fittrack.assessment.entity.FitnessGoal;
import com.fittrack.assessment.entity.FitnessGoal.GoalStatus;
import com.fittrack.assessment.entity.FitnessGoal.GoalType;
import com.fittrack.assessment.mapper.GoalMapper;
import com.fittrack.assessment.repository.FitnessGoalRepository;
import com.fittrack.billing.entity.BillingEnums.Feature;
import com.fittrack.billing.service.EntitlementService;
import com.fittrack.body.entity.WeightRecord;
import com.fittrack.body.repository.WeightRecordRepository;
import com.fittrack.common.exception.BusinessRuleException;
import com.fittrack.common.exception.ResourceNotFoundException;
import com.fittrack.recommendation.calculator.BodyMetrics;
import com.fittrack.security.CurrentUserService;
import com.fittrack.user.entity.User;
import com.fittrack.user.entity.UserProfile;
import com.fittrack.user.repository.UserProfileRepository;
import com.fittrack.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GoalService {

    /** Sensible, safe ranges per goal type: [min, max]. */
    private static final Map<GoalType, BigDecimal[]> RANGES = Map.of(
            GoalType.TARGET_WEIGHT, range("30", "300"),
            GoalType.WEEKLY_WORKOUTS, range("1", "6"),
            GoalType.DAILY_STEPS, range("1000", "30000"),
            GoalType.DAILY_WATER, range("500", "6000"),
            GoalType.SLEEP_HOURS, range("5", "12"),
            GoalType.BODY_FAT, range("5", "50"));

    private final FitnessGoalRepository goalRepository;
    private final WeightRecordRepository weightRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;
    private final GoalMapper mapper;
    private final CurrentUserService currentUser;
    private final EntitlementService entitlements;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<GoalResponse> list(GoalStatus status) {
        Long userId = currentUser.userId();
        List<FitnessGoal> goals = status == null
                ? goalRepository.findByUserIdOrderByStatusAscCreatedAtDesc(userId)
                : goalRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status);
        return goals.stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public GoalResponse get(Long id) {
        return mapper.toResponse(find(id));
    }

    /** Creating a goal of a type that already has an active goal replaces (abandons) the old one. */
    @Transactional
    public GoalResponse create(GoalRequest request) {
        User user = userRepository.getReferenceById(currentUser.userId());
        return mapper.toResponse(createGoal(user, request.goalType(), request.title(),
                request.targetValue(), request.targetDate()));
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public FitnessGoal createGoal(User user, GoalType type, String title, BigDecimal target, LocalDate targetDate) {
        UserProfile profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> ResourceNotFoundException.of("Profile", user.getId()));
        validateTarget(type, target, profile);

        var existingActive = goalRepository.findByUserIdAndGoalTypeAndStatus(user.getId(), type, GoalStatus.ACTIVE);
        if (existingActive.isEmpty()) { // replacing a goal of the same type doesn't count against the limit
            entitlements.checkLimit(user.getId(), Feature.ACTIVE_GOALS,
                    goalRepository.countByUserIdAndStatus(user.getId(), GoalStatus.ACTIVE), "active goals");
        }
        existingActive.ifPresent(existing -> {
                    existing.setStatus(GoalStatus.ABANDONED);
                    goalRepository.saveAndFlush(existing);
                });

        FitnessGoal goal = new FitnessGoal();
        goal.setUser(user);
        goal.setGoalType(type);
        goal.setTitle(StringUtils.hasText(title) ? title.trim() : defaultTitle(type, target));
        goal.setTargetValue(target);
        goal.setUnit(type.unit());
        goal.setStartDate(LocalDate.now(clock.withZone(profile.zoneId())));
        goal.setTargetDate(targetDate);
        if (type == GoalType.TARGET_WEIGHT) {
            goal.setStartValue(weightRepository.findFirstByUserIdOrderByRecordDateDesc(user.getId())
                    .map(WeightRecord::getWeightKg).orElse(null));
        }
        return goalRepository.save(goal);
    }

    @Transactional
    public GoalResponse update(Long id, GoalUpdateRequest request) {
        FitnessGoal goal = find(id);
        if (goal.getStatus() != GoalStatus.ACTIVE) {
            throw new BusinessRuleException("Only active goals can be edited");
        }
        UserProfile profile = profileRepository.findByUserId(currentUser.userId()).orElseThrow();
        validateTarget(goal.getGoalType(), request.targetValue(), profile);
        if (StringUtils.hasText(request.title())) {
            goal.setTitle(request.title().trim());
        }
        goal.setTargetValue(request.targetValue());
        goal.setTargetDate(request.targetDate());
        return mapper.toResponse(goal);
    }

    @Transactional
    public GoalResponse changeStatus(Long id, GoalStatus status) {
        FitnessGoal goal = find(id);
        if (status == GoalStatus.ACTIVE && goal.getStatus() != GoalStatus.ACTIVE) {
            goalRepository.findByUserIdAndGoalTypeAndStatus(currentUser.userId(), goal.getGoalType(), GoalStatus.ACTIVE)
                    .ifPresent(other -> {
                        throw new BusinessRuleException("You already have an active goal of this type");
                    });
            entitlements.checkLimit(currentUser.userId(), Feature.ACTIVE_GOALS,
                    goalRepository.countByUserIdAndStatus(currentUser.userId(), GoalStatus.ACTIVE), "active goals");
        }
        goal.setStatus(status);
        goal.setAchievedAt(status == GoalStatus.ACHIEVED ? clock.instant() : null);
        return mapper.toResponse(goal);
    }

    @Transactional
    public void delete(Long id) {
        goalRepository.delete(find(id));
    }

    private FitnessGoal find(Long id) {
        return goalRepository.findByIdAndUserId(id, currentUser.userId())
                .orElseThrow(() -> ResourceNotFoundException.of("Goal", id));
    }

    private void validateTarget(GoalType type, BigDecimal target, UserProfile profile) {
        BigDecimal[] r = RANGES.get(type);
        if (target.compareTo(r[0]) < 0 || target.compareTo(r[1]) > 0) {
            throw new BusinessRuleException("Target for %s must be between %s and %s %s"
                    .formatted(type, r[0].toPlainString(), r[1].toPlainString(), type.unit()));
        }
        if (type == GoalType.TARGET_WEIGHT && profile.getHeightCm() != null) {
            BigDecimal min = BodyMetrics.minHealthyWeight(profile.getHeightCm());
            if (target.compareTo(min) < 0) {
                throw new BusinessRuleException(("For your height, a target below %s kg would put your BMI under 18.5 "
                        + "(underweight). Please choose a healthier target.").formatted(min.toPlainString()));
            }
        }
    }

    private static String defaultTitle(GoalType type, BigDecimal target) {
        return switch (type) {
            case TARGET_WEIGHT -> "Reach " + target.stripTrailingZeros().toPlainString() + " kg";
            case WEEKLY_WORKOUTS -> target.intValue() + " workouts per week";
            case DAILY_STEPS -> target.intValue() + " steps per day";
            case DAILY_WATER -> target.intValue() + " ml water per day";
            case SLEEP_HOURS -> target.stripTrailingZeros().toPlainString() + " hours of sleep";
            case BODY_FAT -> target.stripTrailingZeros().toPlainString() + "% body fat";
        };
    }

    private static BigDecimal[] range(String min, String max) {
        return new BigDecimal[]{new BigDecimal(min), new BigDecimal(max)};
    }
}
