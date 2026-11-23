package com.fittrack.workout.dto;

import com.fittrack.exercise.dto.ExerciseDtos.ExerciseSummary;
import com.fittrack.workout.entity.PersonalRecord.RecordType;
import com.fittrack.workout.entity.WorkoutPlan.SplitType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class WorkoutDtos {

    private WorkoutDtos() {
    }

    /** SCHEDULED/IN_PROGRESS/COMPLETED/SKIPPED/RESCHEDULED are stored; REST and MISSED are derived. */
    public enum DayStatus { SCHEDULED, IN_PROGRESS, COMPLETED, SKIPPED, RESCHEDULED, MISSED, REST }

    public record PlanExerciseDto(Long id, ExerciseSummary exercise, int sets, Integer repsMin, Integer repsMax,
                                  Integer durationSec, int restSec, Integer targetRpe, String notes) {
    }

    public record PlanDayDto(Long id, int dayOfWeek, String title, String focus, boolean restDay,
                             List<PlanExerciseDto> exercises) {
    }

    public record PlanDto(Long id, String name, SplitType splitType, LocalDate startDate, List<String> explanations,
                          List<PlanDayDto> days) {
    }

    public record SetLogDto(int setNumber, Integer repsDone, BigDecimal weightKg, Integer durationSec, boolean completed) {
    }

    public record SessionExerciseDto(Long planExerciseId, ExerciseSummary exercise, int sets, Integer repsMin,
                                     Integer repsMax, Integer durationSec, int restSec, Integer targetRpe, String notes,
                                     List<SetLogDto> loggedSets, String progressionTip) {
    }

    /** Everything the workout player needs for one date. */
    public record DayWorkoutDto(LocalDate date, Long planDayId, String title, String focus, boolean restDay,
                                DayStatus status, Long sessionId, Instant startedAt, Instant completedAt,
                                Integer perceivedEffort, Integer caloriesEst, int estimatedMinutes,
                                List<SessionExerciseDto> exercises, List<SessionExerciseDto> extraExercises,
                                String recoveryNote) {
    }

    public record CalendarEntryDto(LocalDate date, Long planDayId, String title, boolean restDay, DayStatus status,
                                   Long sessionId, LocalDate rescheduledTo, boolean rescheduledHere) {
    }

    public record SummaryDto(int plannedThisWeek, int completedThisWeek, int skippedThisWeek, int streakDays,
                             long totalCompleted) {
    }

    public record HistoryItemDto(Long sessionId, LocalDate date, String title,
                                 com.fittrack.workout.entity.WorkoutSession.Status status, Instant completedAt,
                                 Integer perceivedEffort, Integer caloriesEst, int setsCompleted, String notes) {
    }

    public record PersonalRecordDto(Long exerciseId, String exerciseName, RecordType type, BigDecimal value,
                                    LocalDate achievedOn) {
    }

    // ---- requests ----

    public record DateRequest(@NotNull LocalDate date, Long planDayId) {
    }

    public record SetLogRequest(@NotNull Long exerciseId, @NotNull @Min(1) @Max(20) Integer setNumber,
                                @Min(0) @Max(500) Integer repsDone,
                                @DecimalMin("0.0") @DecimalMax("500.0") BigDecimal weightKg,
                                @Min(0) @Max(7200) Integer durationSec, boolean completed) {
    }

    public record LogSetsRequest(@NotEmpty @Size(max = 100) List<@Valid SetLogRequest> sets) {
    }

    public record CompleteRequest(@Min(1) @Max(10) Integer perceivedEffort, @Size(max = 1000) String notes) {
    }

    public record SkipRequest(@NotNull LocalDate date, Long planDayId, @Size(max = 500) String reason) {
    }

    public record RescheduleRequest(@NotNull LocalDate date, Long planDayId, @NotNull LocalDate newDate) {
    }

    public record SwapExerciseRequest(@NotNull Long exerciseId) {
    }
}
