package com.fittrack.activity.service;

import com.fittrack.activity.dto.ActivityDtos.RecoveryDto;
import com.fittrack.activity.dto.ActivityDtos.SleepDto;
import com.fittrack.activity.dto.ActivityDtos.SleepRequest;
import com.fittrack.activity.entity.SleepRecord;
import com.fittrack.activity.repository.SleepRecordRepository;
import com.fittrack.common.exception.BusinessRuleException;
import com.fittrack.common.exception.ResourceNotFoundException;
import com.fittrack.recommendation.FitnessContext;
import com.fittrack.recommendation.FitnessContextService;
import com.fittrack.recommendation.RecommendationService;
import com.fittrack.security.CurrentUserService;
import com.fittrack.user.repository.UserRepository;
import com.fittrack.workout.entity.WorkoutSession;
import com.fittrack.workout.repository.WorkoutSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SleepService {

    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");

    private final SleepRecordRepository repository;
    private final WorkoutSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final FitnessContextService contextService;
    private final RecommendationService recommendationService;
    private final CurrentUserService currentUser;

    /**
     * Records the night ending on {@code wakeDate}. If bed time is later than wake time (e.g. 23:00 -> 06:30)
     * the user went to bed the previous evening.
     */
    @Transactional
    public SleepDto record(LocalDate wakeDate, SleepRequest req) {
        Long userId = currentUser.userId();
        FitnessContext ctx = contextService.load(userId);
        if (wakeDate.isAfter(ctx.today())) {
            throw new BusinessRuleException("You can't log sleep for a future date");
        }
        if (ChronoUnit.DAYS.between(wakeDate, ctx.today()) > 365) {
            throw new BusinessRuleException("Sleep can be logged for the past year only");
        }
        ZoneId zone = ctx.zone();
        LocalTime bed = LocalTime.parse(req.bedTime());
        LocalTime wake = LocalTime.parse(req.wakeTime());
        LocalDate bedDate = bed.isAfter(wake) || bed.equals(wake) ? wakeDate.minusDays(1) : wakeDate;
        ZonedDateTime start = ZonedDateTime.of(bedDate, bed, zone);
        ZonedDateTime end = ZonedDateTime.of(wakeDate, wake, zone);
        long minutes = Duration.between(start, end).toMinutes();
        if (minutes < 60 || minutes > 16 * 60) {
            throw new BusinessRuleException("Sleep should be between 1 and 16 hours - please check the times.");
        }

        SleepRecord r = repository.findByUserIdAndSleepDate(userId, wakeDate).orElseGet(() -> {
            SleepRecord n = new SleepRecord();
            n.setUser(userRepository.getReferenceById(userId));
            n.setSleepDate(wakeDate);
            return n;
        });
        r.setSleepStart(start.toInstant());
        r.setWakeTime(end.toInstant());
        r.setDurationMin((int) minutes);
        r.setQuality(req.quality());
        r.setRestDay(req.restDay());
        r.setNotes(StringUtils.hasText(req.notes()) ? req.notes().trim() : null);
        return toDto(repository.save(r), zone);
    }

    @Transactional(readOnly = true)
    public List<SleepDto> history(int days) {
        if (days < 1 || days > 366) {
            throw new BusinessRuleException("Choose between 1 and 366 days");
        }
        FitnessContext ctx = contextService.load(currentUser.userId());
        return repository.findByUserIdAndSleepDateBetweenOrderBySleepDateAsc(ctx.userId(), ctx.today().minusDays(days - 1L), ctx.today())
                .stream().map(r -> toDto(r, ctx.zone())).toList();
    }

    @Transactional
    public void delete(LocalDate date) {
        SleepRecord r = repository.findByUserIdAndSleepDate(currentUser.userId(), date)
                .orElseThrow(() -> new ResourceNotFoundException("No sleep logged for " + date));
        repository.delete(r);
    }

    /** Simple, explainable recovery guidance from recent sleep and workout effort. */
    @Transactional(readOnly = true)
    public RecoveryDto recovery() {
        Long userId = currentUser.userId();
        FitnessContext ctx = contextService.load(userId);
        LocalDate from = ctx.today().minusDays(6);
        List<SleepRecord> sleep = repository.findByUserIdAndSleepDateBetweenOrderBySleepDateAsc(userId, from, ctx.today());
        Double avgSleep = sleep.isEmpty() ? null
                : Math.round(sleep.stream().mapToInt(SleepRecord::getDurationMin).average().orElse(0) / 6.0) / 10.0;
        double goal = recommendationService.targetsFor(userId).sleepGoalHours();
        Double avgEffort = sessionRepository.averageEffort(userId, from, ctx.today());
        int workouts = (int) sessionRepository.findByUserIdAndScheduledDateBetween(userId, from, ctx.today()).stream()
                .filter(s -> s.getStatus() == WorkoutSession.Status.COMPLETED).count();

        List<String> tips = new ArrayList<>();
        String status;
        String intensity;
        if (avgSleep != null && avgSleep < 6) {
            status = "LOW";
            intensity = "Light (effort 5/10) or an easy walk";
            tips.add("Short sleep slows recovery and raises injury risk. Prioritise an earlier bedtime tonight.");
        } else if ((avgEffort != null && avgEffort >= 8.5) || workouts >= 6) {
            status = "MODERATE";
            intensity = "Moderate (effort 6/10) or a rest day";
            tips.add("Your recent workouts were hard. A lighter day helps your muscles adapt.");
        } else if (avgSleep != null && avgSleep < goal - 1) {
            status = "MODERATE";
            intensity = "As planned, but stop a rep or two earlier";
            tips.add("You're a bit short of your sleep goal. Aim for a consistent bedtime.");
        } else {
            status = "GOOD";
            intensity = "As planned";
            tips.add(avgSleep == null ? "Log your sleep to get personalised recovery advice." : "Recovery looks good - keep it up!");
        }
        tips.add("Stretching, a 20-30 minute walk and enough water all support recovery.");
        return new RecoveryDto(avgSleep, goal, avgEffort == null ? null : Math.round(avgEffort * 10) / 10.0,
                workouts, status, intensity, tips);
    }

    private static SleepDto toDto(SleepRecord r, ZoneId zone) {
        return new SleepDto(r.getSleepDate(), r.getSleepStart(), r.getWakeTime(),
                r.getSleepStart().atZone(zone).format(HHMM), r.getWakeTime().atZone(zone).format(HHMM),
                r.getDurationMin(), r.getQuality(), r.isRestDay(), r.getNotes());
    }
}
