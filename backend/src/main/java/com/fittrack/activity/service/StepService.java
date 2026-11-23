package com.fittrack.activity.service;

import com.fittrack.activity.dto.ActivityDtos.ActivityDayDto;
import com.fittrack.activity.dto.ActivityDtos.ActivitySummaryDto;
import com.fittrack.activity.dto.ActivityDtos.HealthImportRequest;
import com.fittrack.activity.dto.ActivityDtos.StepEntryRequest;
import com.fittrack.activity.entity.DailyActivityRecord;
import com.fittrack.activity.entity.DailyActivityRecord.Source;
import com.fittrack.activity.repository.DailyActivityRecordRepository;
import com.fittrack.common.exception.BusinessRuleException;
import com.fittrack.recommendation.FitnessContext;
import com.fittrack.recommendation.FitnessContextService;
import com.fittrack.recommendation.RecommendationService;
import com.fittrack.security.CurrentUserService;
import com.fittrack.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Step tracking. Steps are entered manually in the browser; the import endpoint is the integration point for a
 * mobile companion app reading Google Health Connect / Apple Health. We never claim automatic tracking otherwise.
 */
@Service
@RequiredArgsConstructor
public class StepService {

    private final DailyActivityRecordRepository repository;
    private final UserRepository userRepository;
    private final FitnessContextService contextService;
    private final RecommendationService recommendationService;
    private final CurrentUserService currentUser;

    @Transactional
    public ActivityDayDto record(LocalDate date, StepEntryRequest req) {
        Long userId = currentUser.userId();
        FitnessContext ctx = contextService.load(userId);
        validateDate(date, ctx.today());
        DailyActivityRecord r = upsert(ctx, date, req.steps(), req.distanceM(), req.activeMinutes(), Source.MANUAL);
        return toDto(r, r.getStepGoal());
    }

    @Transactional
    public List<ActivityDayDto> importFromHealthApp(HealthImportRequest req) {
        if (req.source() == Source.MANUAL) {
            throw new BusinessRuleException("Use the normal step entry for manual steps");
        }
        Long userId = currentUser.userId();
        FitnessContext ctx = contextService.load(userId);
        List<ActivityDayDto> out = new ArrayList<>();
        for (var d : req.days()) {
            validateDate(d.date(), ctx.today());
            DailyActivityRecord existing = repository.findByUserIdAndRecordDate(userId, d.date()).orElse(null);
            // Device data wins unless the user manually logged a higher number that day.
            int steps = existing != null && existing.getSource() == Source.MANUAL ? Math.max(existing.getSteps(), d.steps()) : d.steps();
            DailyActivityRecord r = upsert(ctx, d.date(), steps, d.distanceM(), d.activeMinutes(), req.source());
            out.add(toDto(r, r.getStepGoal()));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public ActivitySummaryDto summary(int days) {
        if (days < 1 || days > 366) {
            throw new BusinessRuleException("Choose between 1 and 366 days");
        }
        Long userId = currentUser.userId();
        FitnessContext ctx = contextService.load(userId);
        int goal = recommendationService.targetsFor(userId).stepTarget();
        LocalDate from = ctx.today().minusDays(days - 1L);
        Map<LocalDate, DailyActivityRecord> byDate = repository
                .findByUserIdAndRecordDateBetweenOrderByRecordDateAsc(userId, from, ctx.today()).stream()
                .collect(Collectors.toMap(DailyActivityRecord::getRecordDate, Function.identity()));

        List<ActivityDayDto> history = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(ctx.today()); d = d.plusDays(1)) {
            DailyActivityRecord r = byDate.get(d);
            history.add(r == null ? new ActivityDayDto(d, 0, null, null, null, goal, null, false)
                    : toDto(r, r.getStepGoal() == null ? goal : r.getStepGoal()));
        }
        long total = history.stream().mapToLong(ActivityDayDto::steps).sum();
        int logged = (int) history.stream().filter(h -> h.source() != null).count();
        int avg = logged == 0 ? 0 : (int) Math.round((double) total / logged);
        int met = (int) history.stream().filter(ActivityDayDto::goalMet).count();
        ActivityDayDto today = history.get(history.size() - 1);
        return new ActivitySummaryDto(days, total, avg, met, goal, today, history, insight(avg, goal, met, logged));
    }

    private DailyActivityRecord upsert(FitnessContext ctx, LocalDate date, int steps, Integer distanceM,
                                       Integer activeMinutes, Source source) {
        DailyActivityRecord r = repository.findByUserIdAndRecordDate(ctx.userId(), date).orElseGet(() -> {
            DailyActivityRecord n = new DailyActivityRecord();
            n.setUser(userRepository.getReferenceById(ctx.userId()));
            n.setRecordDate(date);
            return n;
        });
        double heightCm = ctx.profile().getHeightCm() == null ? 165 : ctx.profile().getHeightCm().doubleValue();
        double weight = ctx.weightOr(70);
        r.setSteps(steps);
        // Typical walking stride is ~41.5% of height.
        r.setDistanceM(distanceM != null ? distanceM : (int) Math.round(steps * heightCm * 0.415 / 100.0));
        r.setActiveMinutes(activeMinutes != null ? activeMinutes : (int) Math.round(steps / 100.0));
        // ~0.04 kcal per step for a 70 kg person, scaled by body weight. Estimate only.
        r.setCaloriesEst((int) Math.round(steps * 0.04 * weight / 70.0));
        if (r.getStepGoal() == null || date.equals(ctx.today())) {
            r.setStepGoal(recommendationService.targetsFor(ctx.userId()).stepTarget());
        }
        r.setSource(source);
        return repository.save(r);
    }

    private static void validateDate(LocalDate date, LocalDate today) {
        if (date.isAfter(today)) {
            throw new BusinessRuleException("You can't log steps for a future date");
        }
        if (ChronoUnit.DAYS.between(date, today) > 365) {
            throw new BusinessRuleException("Steps can be logged for the past year only");
        }
    }

    private static ActivityDayDto toDto(DailyActivityRecord r, Integer goal) {
        int g = goal == null ? 0 : goal;
        return new ActivityDayDto(r.getRecordDate(), r.getSteps(), r.getDistanceM(), r.getActiveMinutes(), r.getCaloriesEst(),
                g, r.getSource(), g > 0 && r.getSteps() >= g);
    }

    private static String insight(int avg, int goal, int metDays, int loggedDays) {
        if (loggedDays == 0) return "Log your steps each evening - most phones show today's count in their health app.";
        if (avg >= goal) return "You're averaging above your goal. When it feels easy, we'll raise it by about 1,000 steps.";
        int gap = goal - avg;
        if (gap <= 1500) return "You're close! A 10-15 minute walk after a meal adds about 1,000-1,500 steps.";
        return "Build up gradually: add a short walk each day and take the stairs when you can.";
    }
}
