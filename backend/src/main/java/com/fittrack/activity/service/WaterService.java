package com.fittrack.activity.service;

import com.fittrack.activity.dto.ActivityDtos.WaterDayDto;
import com.fittrack.activity.dto.ActivityDtos.WaterEntryDto;
import com.fittrack.activity.dto.ActivityDtos.WaterGoalRequest;
import com.fittrack.activity.dto.ActivityDtos.WaterHistoryDayDto;
import com.fittrack.activity.entity.WaterDailyGoal;
import com.fittrack.activity.entity.WaterIntakeEntry;
import com.fittrack.activity.repository.WaterDailyGoalRepository;
import com.fittrack.activity.repository.WaterIntakeEntryRepository;
import com.fittrack.common.exception.BusinessRuleException;
import com.fittrack.common.exception.ResourceNotFoundException;
import com.fittrack.recommendation.FitnessContextService;
import com.fittrack.recommendation.RecommendationService;
import com.fittrack.recommendation.dto.RecommendationDtos.DailyTargets;
import com.fittrack.security.CurrentUserService;
import com.fittrack.user.repository.UserRepository;
import com.fittrack.workout.entity.WorkoutPlan;
import com.fittrack.workout.repository.WorkoutPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WaterService {

    /** Drinking far more than this in a day can be dangerous (hyponatremia). */
    private static final int DAILY_SAFETY_CAP_ML = 8000;

    private final WaterIntakeEntryRepository entryRepository;
    private final WaterDailyGoalRepository goalRepository;
    private final UserRepository userRepository;
    private final FitnessContextService contextService;
    private final RecommendationService recommendationService;
    private final WorkoutPlanRepository planRepository;
    private final CurrentUserService currentUser;
    private final Clock clock;

    @Transactional
    public WaterDayDto add(int amountMl, LocalDate requestedDate) {
        Long userId = currentUser.userId();
        LocalDate today = contextService.today(userId);
        LocalDate date = requestedDate == null ? today : requestedDate;
        validateDate(date, today);
        int total = entryRepository.totalFor(userId, date);
        if (total + amountMl > DAILY_SAFETY_CAP_ML) {
            throw new BusinessRuleException("That would be more than 8 litres today. Drinking very large amounts can be "
                    + "harmful - please check the amount.");
        }
        WaterIntakeEntry e = new WaterIntakeEntry();
        e.setUser(userRepository.getReferenceById(userId));
        e.setRecordDate(date);
        e.setAmountMl(amountMl);
        e.setLoggedAt(clock.instant());
        entryRepository.save(e);
        return day(date);
    }

    @Transactional
    public WaterDayDto delete(Long entryId) {
        Long userId = currentUser.userId();
        WaterIntakeEntry e = entryRepository.findByIdAndUserId(entryId, userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Water entry", entryId));
        LocalDate date = e.getRecordDate();
        entryRepository.delete(e);
        entryRepository.flush();
        return day(date);
    }

    @Transactional
    public WaterDayDto setGoal(LocalDate date, WaterGoalRequest req) {
        Long userId = currentUser.userId();
        validateDate(date, contextService.today(userId).plusDays(7));
        WaterDailyGoal g = goalRepository.findByUserIdAndRecordDate(userId, date).orElseGet(() -> {
            WaterDailyGoal n = new WaterDailyGoal();
            n.setUser(userRepository.getReferenceById(userId));
            n.setRecordDate(date);
            return n;
        });
        g.setGoalMl(req.goalMl());
        g.setAdjustmentReason(StringUtils.hasText(req.reason()) ? req.reason().trim() : null);
        goalRepository.save(g);
        return day(date);
    }

    @Transactional
    public WaterDayDto day(LocalDate requestedDate) {
        Long userId = currentUser.userId();
        LocalDate date = requestedDate == null ? contextService.today(userId) : requestedDate;
        List<WaterIntakeEntry> entries = entryRepository.findByUserIdAndRecordDateOrderByLoggedAtAsc(userId, date);
        int total = entries.stream().mapToInt(WaterIntakeEntry::getAmountMl).sum();
        WaterDailyGoal custom = goalRepository.findByUserIdAndRecordDate(userId, date).orElse(null);
        DailyTargets targets = recommendationService.targetsFor(userId);
        boolean trainingDay = isTrainingDay(date);
        int goal = custom != null ? custom.getGoalMl() : (trainingDay ? targets.waterTrainingDayMl() : targets.waterMl());

        List<String> guidance = new ArrayList<>();
        if (custom == null && trainingDay) guidance.add("Workout day: we added 500 ml to replace what you lose through sweat.");
        guidance.add("Sip regularly through the day rather than drinking a lot at once.");
        guidance.add("Hot or humid weather, or a long workout? Add 500-1,000 ml with the goal adjuster.");
        guidance.add("If you have a kidney or heart condition, follow your doctor's fluid advice instead.");

        return new WaterDayDto(date, goal, total, Math.max(0, goal - total), goal == 0 ? 0 : Math.min(100, total * 100 / goal),
                custom != null, custom == null ? null : custom.getAdjustmentReason(),
                entries.stream().map(e -> new WaterEntryDto(e.getId(), e.getAmountMl(), e.getLoggedAt())).toList(), guidance);
    }

    @Transactional(readOnly = true)
    public List<WaterHistoryDayDto> history(int days) {
        if (days < 1 || days > 366) {
            throw new BusinessRuleException("Choose between 1 and 366 days");
        }
        Long userId = currentUser.userId();
        LocalDate to = contextService.today(userId);
        LocalDate from = to.minusDays(days - 1L);
        Map<LocalDate, Integer> totals = entryRepository.dailyTotals(userId, from, to).stream()
                .collect(Collectors.toMap(r -> (LocalDate) r[0], r -> ((Number) r[1]).intValue()));
        Map<LocalDate, Integer> custom = goalRepository.findByUserIdAndRecordDateBetween(userId, from, to).stream()
                .collect(Collectors.toMap(WaterDailyGoal::getRecordDate, WaterDailyGoal::getGoalMl));
        int baseGoal = recommendationService.targetsFor(userId).waterMl();
        List<WaterHistoryDayDto> out = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            out.add(new WaterHistoryDayDto(d, totals.getOrDefault(d, 0), custom.getOrDefault(d, baseGoal)));
        }
        return out;
    }

    /** Reads the active plan directly (no plan generation, no exceptions that would poison the transaction). */
    private boolean isTrainingDay(LocalDate date) {
        Long userId = currentUser.userId();
        return planRepository.findByUserIdAndStatus(userId, WorkoutPlan.Status.ACTIVE)
                .flatMap(p -> p.dayFor(date.getDayOfWeek().getValue()))
                .map(d -> !d.isRestDay())
                .orElse(false);
    }

    private static void validateDate(LocalDate date, LocalDate latest) {
        if (date.isAfter(latest)) {
            throw new BusinessRuleException("That date is too far in the future");
        }
        if (ChronoUnit.DAYS.between(date, latest) > 366) {
            throw new BusinessRuleException("That date is too far in the past");
        }
    }
}
