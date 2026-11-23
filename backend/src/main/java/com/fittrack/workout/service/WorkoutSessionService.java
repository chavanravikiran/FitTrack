package com.fittrack.workout.service;

import com.fittrack.activity.entity.SleepRecord;
import com.fittrack.activity.repository.SleepRecordRepository;
import com.fittrack.common.api.PageResponse;
import com.fittrack.common.exception.BusinessRuleException;
import com.fittrack.common.exception.ResourceNotFoundException;
import com.fittrack.exercise.entity.Exercise;
import com.fittrack.exercise.repository.ExerciseRepository;
import com.fittrack.exercise.service.ExerciseService;
import com.fittrack.recommendation.FitnessContext;
import com.fittrack.recommendation.FitnessContextService;
import com.fittrack.security.CurrentUserService;
import com.fittrack.user.repository.UserRepository;
import com.fittrack.workout.dto.WorkoutDtos.CalendarEntryDto;
import com.fittrack.workout.dto.WorkoutDtos.CompleteRequest;
import com.fittrack.workout.dto.WorkoutDtos.DayStatus;
import com.fittrack.workout.dto.WorkoutDtos.DayWorkoutDto;
import com.fittrack.workout.dto.WorkoutDtos.HistoryItemDto;
import com.fittrack.workout.dto.WorkoutDtos.PersonalRecordDto;
import com.fittrack.workout.dto.WorkoutDtos.SessionExerciseDto;
import com.fittrack.workout.dto.WorkoutDtos.SetLogDto;
import com.fittrack.workout.dto.WorkoutDtos.SetLogRequest;
import com.fittrack.workout.dto.WorkoutDtos.SummaryDto;
import com.fittrack.workout.entity.PersonalRecord;
import com.fittrack.workout.entity.PersonalRecord.RecordType;
import com.fittrack.workout.entity.WorkoutExercise;
import com.fittrack.workout.entity.WorkoutPlan;
import com.fittrack.workout.entity.WorkoutPlanDay;
import com.fittrack.workout.entity.WorkoutSession;
import com.fittrack.workout.entity.WorkoutSession.Status;
import com.fittrack.workout.entity.WorkoutSessionExercise;
import com.fittrack.workout.repository.PersonalRecordRepository;
import com.fittrack.workout.repository.WorkoutSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkoutSessionService {

    private static final int MAX_BACKFILL_DAYS = 7;
    private static final int MAX_RESCHEDULE_DAYS = 14;

    private final WorkoutPlanService planService;
    private final WorkoutSessionRepository sessionRepository;
    private final PersonalRecordRepository recordRepository;
    private final ExerciseRepository exerciseRepository;
    private final SleepRecordRepository sleepRepository;
    private final UserRepository userRepository;
    private final FitnessContextService contextService;
    private final CurrentUserService currentUser;
    private final Clock clock;

    // ---------------------------------------------------------------- read

    /** @param requestedDate null = today in the user's time zone */
    @Transactional
    public DayWorkoutDto day(LocalDate requestedDate, Long planDayId) {
        Long userId = currentUser.userId();
        WorkoutPlan plan = planService.activePlanEntity(userId);
        LocalDate today = contextService.today(userId);
        LocalDate date = requestedDate == null ? today : requestedDate;
        List<WorkoutSession> sessions = sessionRepository.findByUserIdAndScheduledDateBetween(userId, date, date);

        WorkoutPlanDay planDay = resolvePlanDay(plan, date, planDayId, sessions);
        Optional<WorkoutSession> session = planDay == null ? Optional.empty() : sessions.stream()
                .filter(s -> s.getPlanDay() != null && s.getPlanDay().getId().equals(planDay.getId())).findFirst();

        if (planDay == null) {
            return new DayWorkoutDto(date, null, "No workout", null, true, DayStatus.REST, null, null, null, null, null,
                    0, List.of(), List.of(), null);
        }
        DayStatus status = status(planDay, session.orElse(null), date, today);
        Map<Long, List<WorkoutSessionExercise>> logged = new LinkedHashMap<>();
        session.ifPresent(s -> s.getSets().forEach(x ->
                logged.computeIfAbsent(x.getExercise().getId(), k -> new ArrayList<>()).add(x)));

        Set<Long> planned = new HashSet<>();
        List<SessionExerciseDto> exercises = new ArrayList<>();
        for (WorkoutExercise we : planDay.getExercises().stream().sorted(Comparator.comparingInt(WorkoutExercise::getSortOrder)).toList()) {
            planned.add(we.getExercise().getId());
            exercises.add(toSessionExercise(we, logged.getOrDefault(we.getExercise().getId(), List.of()),
                    status == DayStatus.COMPLETED ? null : progressionTip(userId, we)));
        }
        // Exercises logged that aren't in the plan (alternatives the user swapped in for this session).
        List<SessionExerciseDto> extra = logged.entrySet().stream()
                .filter(e -> !planned.contains(e.getKey()))
                .map(e -> new SessionExerciseDto(null, ExerciseService.toSummary(e.getValue().get(0).getExercise()),
                        e.getValue().size(), null, null, null, 60, null, "Alternative you chose for this session",
                        e.getValue().stream().map(WorkoutSessionService::toSetDto).toList(), null))
                .toList();

        WorkoutSession s = session.orElse(null);
        return new DayWorkoutDto(date, planDay.getId(), planDay.getTitle(), planDay.getFocus(), planDay.isRestDay(), status,
                s == null ? null : s.getId(), s == null ? null : s.getStartedAt(), s == null ? null : s.getCompletedAt(),
                s == null ? null : s.getPerceivedEffort(), s == null ? null : s.getCaloriesEst(),
                planDay.isRestDay() ? 0 : WorkoutMapper.estimateMinutes(planDay.getExercises()),
                exercises, extra, planDay.isRestDay() ? null : recoveryNote(userId, date));
    }

    @Transactional
    public List<CalendarEntryDto> calendar(LocalDate from, LocalDate to) {
        if (to.isBefore(from) || ChronoUnit.DAYS.between(from, to) > 62) {
            throw new BusinessRuleException("Choose a date range of up to 62 days");
        }
        Long userId = currentUser.userId();
        WorkoutPlan plan = planService.activePlanEntity(userId);
        LocalDate today = contextService.today(userId);
        List<WorkoutSession> sessions = sessionRepository.findByUserIdAndScheduledDateBetween(userId, from, to);
        Map<String, WorkoutSession> byKey = sessions.stream().filter(s -> s.getPlanDay() != null)
                .collect(Collectors.toMap(s -> key(s.getScheduledDate(), s.getPlanDay().getId()), Function.identity(), (a, b) -> a));

        List<CalendarEntryDto> entries = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            final LocalDate date = d;
            plan.dayFor(d.getDayOfWeek().getValue()).ifPresent(pd -> {
                WorkoutSession s = byKey.get(key(date, pd.getId()));
                entries.add(new CalendarEntryDto(date, pd.getId(), pd.getTitle(), pd.isRestDay(), status(pd, s, date, today),
                        s == null ? null : s.getId(), s == null ? null : s.getRescheduledTo(), false));
            });
            // Sessions moved onto this date from another day.
            sessions.stream()
                    .filter(s -> s.getScheduledDate().equals(date) && s.getPlanDay() != null
                            && s.getPlanDay().getDayOfWeek() != date.getDayOfWeek().getValue())
                    .forEach(s -> entries.add(new CalendarEntryDto(date, s.getPlanDay().getId(), s.getPlanDay().getTitle(),
                            false, status(s.getPlanDay(), s, date, today), s.getId(), null, true)));
        }
        return entries;
    }

    @Transactional
    public SummaryDto summary() {
        Long userId = currentUser.userId();
        LocalDate today = contextService.today(userId);
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        List<CalendarEntryDto> week = calendar(weekStart, weekStart.plusDays(6));
        int planned = (int) week.stream().filter(e -> !e.restDay() && e.status() != DayStatus.RESCHEDULED).count();
        int completed = (int) week.stream().filter(e -> e.status() == DayStatus.COMPLETED).count();
        int skipped = (int) week.stream().filter(e -> e.status() == DayStatus.SKIPPED).count();
        return new SummaryDto(planned, completed, skipped, streak(today),
                sessionRepository.countByUserIdAndStatus(userId, Status.COMPLETED));
    }

    @Transactional(readOnly = true)
    public PageResponse<HistoryItemDto> history(int page, int size) {
        return PageResponse.of(sessionRepository.findByUserIdAndStatusInOrderByScheduledDateDesc(currentUser.userId(),
                        EnumSet.of(Status.COMPLETED, Status.SKIPPED, Status.RESCHEDULED), PageRequest.of(page, size)),
                s -> new HistoryItemDto(s.getId(), s.getScheduledDate(),
                        s.getPlanDay() == null ? "Workout" : s.getPlanDay().getTitle(), s.getStatus(), s.getCompletedAt(),
                        s.getPerceivedEffort(), s.getCaloriesEst(),
                        (int) s.getSets().stream().filter(WorkoutSessionExercise::isCompleted).count(), s.getNotes()));
    }

    @Transactional(readOnly = true)
    public List<PersonalRecordDto> personalRecords() {
        return recordRepository.findByUserIdOrderByAchievedOnDesc(currentUser.userId()).stream()
                .map(r -> new PersonalRecordDto(r.getExercise().getId(), r.getExercise().getName(), r.getRecordType(),
                        r.getValue(), r.getAchievedOn()))
                .toList();
    }

    // ---------------------------------------------------------------- write

    @Transactional
    public DayWorkoutDto start(LocalDate date, Long planDayId) {
        Long userId = currentUser.userId();
        LocalDate today = contextService.today(userId);
        if (date.isAfter(today)) {
            throw new BusinessRuleException("You can't start a future workout. Reschedule it to today instead.");
        }
        if (date.isBefore(today.minusDays(MAX_BACKFILL_DAYS))) {
            throw new BusinessRuleException("Workouts can be logged up to " + MAX_BACKFILL_DAYS + " days back.");
        }
        WorkoutSession s = sessionFor(userId, date, planDayId, true);
        if (s.getStatus() == Status.COMPLETED) {
            throw new BusinessRuleException("This workout is already completed.");
        }
        if (s.getStatus() == Status.RESCHEDULED) {
            throw new BusinessRuleException("This workout was moved to " + s.getRescheduledTo() + ".");
        }
        if (s.getStatus() != Status.IN_PROGRESS) {
            s.setStatus(Status.IN_PROGRESS);
            s.setStartedAt(clock.instant());
        }
        return day(date, s.getPlanDay().getId());
    }

    @Transactional
    public DayWorkoutDto logSets(Long sessionId, List<SetLogRequest> sets) {
        WorkoutSession s = ownedSession(sessionId);
        if (s.getStatus() != Status.IN_PROGRESS && s.getStatus() != Status.COMPLETED) {
            throw new BusinessRuleException("Start the workout before logging sets.");
        }
        Map<Long, Exercise> exercises = new HashMap<>();
        for (SetLogRequest req : sets) {
            Exercise ex = exercises.computeIfAbsent(req.exerciseId(), id -> exerciseRepository.findById(id)
                    .filter(Exercise::isActive).orElseThrow(() -> ResourceNotFoundException.of("Exercise", id)));
            WorkoutSessionExercise row = s.getSets().stream()
                    .filter(x -> x.getExercise().getId().equals(ex.getId()) && x.getSetNumber() == req.setNumber())
                    .findFirst()
                    .orElseGet(() -> {
                        WorkoutSessionExercise n = new WorkoutSessionExercise();
                        n.setSession(s);
                        n.setExercise(ex);
                        n.setSetNumber(req.setNumber());
                        s.getSets().add(n);
                        return n;
                    });
            row.setRepsDone(req.repsDone());
            row.setWeightKg(req.weightKg());
            row.setDurationSec(req.durationSec());
            row.setCompleted(req.completed());
        }
        sessionRepository.saveAndFlush(s);
        return day(s.getScheduledDate(), s.getPlanDay() == null ? null : s.getPlanDay().getId());
    }

    @Transactional
    public DayWorkoutDto complete(Long sessionId, CompleteRequest req) {
        WorkoutSession s = ownedSession(sessionId);
        if (s.getStatus() != Status.IN_PROGRESS) {
            throw new BusinessRuleException("Only a workout in progress can be completed.");
        }
        Instant now = clock.instant();
        s.setStatus(Status.COMPLETED);
        s.setCompletedAt(now);
        s.setPerceivedEffort(req == null ? null : req.perceivedEffort());
        if (req != null && StringUtils.hasText(req.notes())) {
            s.setNotes(req.notes().trim());
        }
        s.setCaloriesEst(estimateCalories(s, now));
        updatePersonalRecords(s);
        return day(s.getScheduledDate(), s.getPlanDay() == null ? null : s.getPlanDay().getId());
    }

    @Transactional
    public CalendarEntryDto skip(LocalDate date, Long planDayId, String reason) {
        Long userId = currentUser.userId();
        WorkoutSession s = sessionFor(userId, date, planDayId, true);
        if (s.getStatus() == Status.COMPLETED) {
            throw new BusinessRuleException("A completed workout can't be skipped.");
        }
        s.setStatus(Status.SKIPPED);
        s.setNotes(StringUtils.hasText(reason) ? reason.trim() : null);
        LocalDate today = contextService.today(userId);
        return new CalendarEntryDto(date, s.getPlanDay().getId(), s.getPlanDay().getTitle(), false,
                status(s.getPlanDay(), s, date, today), s.getId(), null, false);
    }

    @Transactional
    public CalendarEntryDto reschedule(LocalDate date, Long planDayId, LocalDate newDate) {
        Long userId = currentUser.userId();
        LocalDate today = contextService.today(userId);
        if (newDate.isBefore(today) || newDate.isAfter(today.plusDays(MAX_RESCHEDULE_DAYS))) {
            throw new BusinessRuleException("Pick a new date between today and " + MAX_RESCHEDULE_DAYS + " days from now.");
        }
        if (newDate.equals(date)) {
            throw new BusinessRuleException("Choose a different date.");
        }
        WorkoutSession original = sessionFor(userId, date, planDayId, true);
        if (original.getStatus() == Status.COMPLETED || original.getStatus() == Status.IN_PROGRESS) {
            throw new BusinessRuleException("This workout has already been started.");
        }
        WorkoutPlanDay pd = original.getPlanDay();
        sessionRepository.findByUserIdAndPlanDayIdAndScheduledDate(userId, pd.getId(), newDate).ifPresent(x -> {
            throw new BusinessRuleException("This workout is already scheduled on " + newDate + ".");
        });
        original.setStatus(Status.RESCHEDULED);
        original.setRescheduledTo(newDate);

        WorkoutSession moved = new WorkoutSession();
        moved.setUser(original.getUser());
        moved.setPlanDay(pd);
        moved.setScheduledDate(newDate);
        moved.setStatus(Status.SCHEDULED);
        moved.setNotes("Moved from " + date);
        sessionRepository.save(moved);
        return new CalendarEntryDto(newDate, pd.getId(), pd.getTitle(), false, DayStatus.SCHEDULED, moved.getId(), null, true);
    }

    // ---------------------------------------------------------------- helpers

    /** Finds (or creates) the stored session for a plan day on a date. */
    private WorkoutSession sessionFor(Long userId, LocalDate date, Long planDayId, boolean create) {
        WorkoutPlan plan = planService.activePlanEntity(userId);
        List<WorkoutSession> sessions = sessionRepository.findByUserIdAndScheduledDateBetween(userId, date, date);
        WorkoutPlanDay pd = resolvePlanDay(plan, date, planDayId, sessions);
        if (pd == null || pd.isRestDay()) {
            throw new BusinessRuleException("There is no workout planned on this day.");
        }
        Optional<WorkoutSession> existing = sessions.stream()
                .filter(s -> s.getPlanDay() != null && s.getPlanDay().getId().equals(pd.getId())).findFirst();
        if (existing.isPresent() || !create) {
            return existing.orElseThrow(() -> new ResourceNotFoundException("Workout not found"));
        }
        WorkoutSession s = new WorkoutSession();
        s.setUser(userRepository.getReferenceById(userId));
        s.setPlanDay(pd);
        s.setScheduledDate(date);
        return sessionRepository.save(s);
    }

    /** Explicit plan day if given (must belong to the plan); else a session moved here; else the weekday's day. */
    private WorkoutPlanDay resolvePlanDay(WorkoutPlan plan, LocalDate date, Long planDayId, List<WorkoutSession> sessions) {
        if (planDayId != null) {
            return plan.getDays().stream().filter(d -> d.getId().equals(planDayId)).findFirst()
                    .orElseThrow(() -> ResourceNotFoundException.of("Plan day", planDayId));
        }
        WorkoutPlanDay regular = plan.dayFor(date.getDayOfWeek().getValue()).orElse(null);
        if (regular != null && !regular.isRestDay()) {
            return regular;
        }
        return sessions.stream()
                .filter(s -> s.getPlanDay() != null && s.getStatus() != Status.RESCHEDULED
                        && plan.getDays().contains(s.getPlanDay()))
                .map(WorkoutSession::getPlanDay)
                .findFirst()
                .orElse(regular);
    }

    private DayStatus status(WorkoutPlanDay pd, WorkoutSession s, LocalDate date, LocalDate today) {
        if (s != null) {
            return DayStatus.valueOf(s.getStatus().name());
        }
        if (pd.isRestDay()) return DayStatus.REST;
        return date.isBefore(today) ? DayStatus.MISSED : DayStatus.SCHEDULED;
    }

    private SessionExerciseDto toSessionExercise(WorkoutExercise we, List<WorkoutSessionExercise> logged, String tip) {
        return new SessionExerciseDto(we.getId(), ExerciseService.toSummary(we.getExercise()), we.getSets(), we.getRepsMin(),
                we.getRepsMax(), we.getDurationSec(), we.getRestSec(), we.getTargetRpe(), we.getNotes(),
                logged.stream().sorted(Comparator.comparingInt(WorkoutSessionExercise::getSetNumber))
                        .map(WorkoutSessionService::toSetDto).toList(), tip);
    }

    private static SetLogDto toSetDto(WorkoutSessionExercise x) {
        return new SetLogDto(x.getSetNumber(), x.getRepsDone(), x.getWeightKg(), x.getDurationSec(), x.isCompleted());
    }

    /** Double progression: once every set reaches the top of the range, add a little weight or reps. */
    String progressionTip(Long userId, WorkoutExercise we) {
        List<WorkoutSession> last = sessionRepository.findCompletedWithExercise(userId, we.getExercise().getId(), PageRequest.of(0, 1));
        if (last.isEmpty()) return null;
        WorkoutSession s = last.get(0);
        List<WorkoutSessionExercise> sets = s.getSets().stream()
                .filter(x -> x.getExercise().getId().equals(we.getExercise().getId())).toList();
        if (sets.isEmpty()) return null;
        if (s.getPerceivedEffort() != null && s.getPerceivedEffort() >= 9) {
            return "Your last session felt very hard - keep the same weight and focus on clean reps.";
        }
        boolean allDone = sets.size() >= we.getSets() && sets.stream().allMatch(WorkoutSessionExercise::isCompleted);
        if (!allDone) return null;
        if (we.getDurationSec() != null) {
            boolean hitTime = sets.stream().allMatch(x -> x.getDurationSec() != null && x.getDurationSec() >= we.getDurationSec());
            return hitTime ? "You held the full time last session - try adding 5-10 seconds." : null;
        }
        if (we.getRepsMax() != null && sets.stream().allMatch(x -> x.getRepsDone() != null && x.getRepsDone() >= we.getRepsMax())) {
            boolean weighted = sets.stream().anyMatch(x -> x.getWeightKg() != null && x.getWeightKg().signum() > 0);
            return weighted
                    ? "You hit " + we.getRepsMax() + " reps on every set last time - add 1-2.5 kg and aim for " + we.getRepsMin() + "+ reps."
                    : "You hit " + we.getRepsMax() + " reps on every set last time - add 2 reps or slow down each rep.";
        }
        return null;
    }

    private String recoveryNote(Long userId, LocalDate date) {
        List<SleepRecord> sleep = sleepRepository.findByUserIdAndSleepDateBetweenOrderBySleepDateAsc(userId, date.minusDays(2), date);
        if (sleep.size() >= 2) {
            double avgHours = sleep.stream().mapToInt(SleepRecord::getDurationMin).average().orElse(480) / 60.0;
            if (avgHours < 6) {
                return "You've slept under 6 hours recently. Keep today's effort light (about 5-6/10) or take an easy walk instead.";
            }
        }
        return null;
    }

    private Integer estimateCalories(WorkoutSession s, Instant completedAt) {
        FitnessContext ctx = contextService.load(s.getUser().getId());
        double weight = ctx.weightOr(70);
        double avgMet = s.getSets().stream().filter(WorkoutSessionExercise::isCompleted)
                .map(x -> x.getExercise().met()).mapToDouble(Double::doubleValue).average()
                .orElse(s.getPlanDay() == null ? 4.0 : s.getPlanDay().getExercises().stream()
                        .mapToDouble(e -> e.getExercise().met()).average().orElse(4.0));
        long minutes = s.getStartedAt() == null ? 30 : Duration.between(s.getStartedAt(), completedAt).toMinutes();
        minutes = Math.max(10, Math.min(180, minutes));
        // kcal = MET x weight(kg) x hours
        return (int) Math.round(avgMet * weight * minutes / 60.0);
    }

    private void updatePersonalRecords(WorkoutSession s) {
        Map<Exercise, List<WorkoutSessionExercise>> byExercise = s.getSets().stream()
                .filter(WorkoutSessionExercise::isCompleted)
                .collect(Collectors.groupingBy(WorkoutSessionExercise::getExercise));
        byExercise.forEach((ex, sets) -> {
            sets.stream().map(WorkoutSessionExercise::getWeightKg).filter(Objects::nonNull).filter(w -> w.signum() > 0)
                    .max(BigDecimal::compareTo).ifPresent(v -> upsertRecord(s, ex, RecordType.MAX_WEIGHT, v));
            boolean weighted = sets.stream().anyMatch(x -> x.getWeightKg() != null && x.getWeightKg().signum() > 0);
            if (!weighted) {
                sets.stream().map(WorkoutSessionExercise::getRepsDone).filter(Objects::nonNull).max(Integer::compare)
                        .ifPresent(v -> upsertRecord(s, ex, RecordType.MAX_REPS, BigDecimal.valueOf(v)));
            }
            sets.stream().map(WorkoutSessionExercise::getDurationSec).filter(Objects::nonNull).max(Integer::compare)
                    .ifPresent(v -> upsertRecord(s, ex, RecordType.LONGEST_DURATION, BigDecimal.valueOf(v)));
        });
    }

    private void upsertRecord(WorkoutSession s, Exercise ex, RecordType type, BigDecimal value) {
        Long userId = s.getUser().getId();
        PersonalRecord r = recordRepository.findByUserIdAndExerciseIdAndRecordType(userId, ex.getId(), type).orElse(null);
        if (r != null && r.getValue().compareTo(value) >= 0) return;
        if (r == null) {
            r = new PersonalRecord();
            r.setUser(s.getUser());
            r.setExercise(ex);
            r.setRecordType(type);
        }
        r.setValue(value);
        r.setAchievedOn(s.getScheduledDate());
        r.setSession(s);
        recordRepository.save(r);
    }

    /** Consecutive planned training days completed, counting back from today (today counts only if done). */
    private int streak(LocalDate today) {
        List<CalendarEntryDto> entries = calendar(today.minusDays(60), today);
        Map<LocalDate, List<CalendarEntryDto>> byDate = entries.stream().collect(Collectors.groupingBy(CalendarEntryDto::date));
        int streak = 0;
        for (LocalDate d = today; !d.isBefore(today.minusDays(60)); d = d.minusDays(1)) {
            List<CalendarEntryDto> day = byDate.getOrDefault(d, List.of()).stream()
                    .filter(e -> !e.restDay() && e.status() != DayStatus.RESCHEDULED).toList();
            if (day.isEmpty()) continue;
            boolean done = day.stream().anyMatch(e -> e.status() == DayStatus.COMPLETED);
            if (done) {
                streak++;
            } else if (!d.equals(today)) {
                break;
            }
        }
        return streak;
    }

    private WorkoutSession ownedSession(Long id) {
        return sessionRepository.findByIdAndUserId(id, currentUser.userId())
                .orElseThrow(() -> ResourceNotFoundException.of("Workout session", id));
    }

    private static String key(LocalDate date, Long planDayId) {
        return date + "|" + planDayId;
    }
}
