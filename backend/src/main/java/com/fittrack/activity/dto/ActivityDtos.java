package com.fittrack.activity.dto;

import com.fittrack.activity.entity.DailyActivityRecord.Source;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class ActivityDtos {

    private ActivityDtos() {
    }

    // ---------------- steps ----------------

    public record StepEntryRequest(@NotNull @Min(0) @Max(100_000) Integer steps,
                                   @Min(0) @Max(200_000) Integer distanceM,
                                   @Min(0) @Max(1440) Integer activeMinutes) {
    }

    public record ActivityDayDto(LocalDate date, int steps, Integer distanceM, Integer activeMinutes, Integer caloriesEst,
                                 int stepGoal, Source source, boolean goalMet) {
    }

    public record ActivitySummaryDto(int days, long totalSteps, int averageSteps, int goalMetDays, int stepGoal,
                                     ActivityDayDto today, List<ActivityDayDto> history, String insight) {
    }

    public record HealthImportDay(@NotNull LocalDate date, @NotNull @Min(0) @Max(100_000) Integer steps,
                                  @Min(0) @Max(200_000) Integer distanceM, @Min(0) @Max(1440) Integer activeMinutes) {
    }

    /** Integration layer for a future mobile companion app (Health Connect / Apple Health). */
    public record HealthImportRequest(@NotNull Source source, @NotEmpty @Size(max = 31) List<@Valid HealthImportDay> days) {
    }

    // ---------------- water ----------------

    public record WaterAddRequest(@NotNull @Min(1) @Max(3000) Integer amountMl, LocalDate date) {
    }

    public record WaterGoalRequest(@NotNull @Min(500) @Max(6000) Integer goalMl, @Size(max = 255) String reason) {
    }

    public record WaterEntryDto(Long id, int amountMl, Instant loggedAt) {
    }

    public record WaterDayDto(LocalDate date, int goalMl, int totalMl, int remainingMl, int percent,
                              boolean customGoal, String goalReason, List<WaterEntryDto> entries, List<String> guidance) {
    }

    public record WaterHistoryDayDto(LocalDate date, int totalMl, int goalMl) {
    }

    // ---------------- sleep ----------------

    public record SleepRequest(
            @NotNull @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "use HH:mm") String bedTime,
            @NotNull @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "use HH:mm") String wakeTime,
            @Min(1) @Max(5) Integer quality,
            boolean restDay,
            @Size(max = 255) String notes) {
    }

    public record SleepDto(LocalDate date, Instant sleepStart, Instant wakeTime, String bedTimeLocal, String wakeTimeLocal,
                           int durationMin, Integer quality, boolean restDay, String notes) {
    }

    public record RecoveryDto(Double averageSleepHours7d, double sleepGoalHours, Double averageEffort7d,
                              int workoutsLast7Days, String status, String suggestedIntensity, List<String> tips) {
    }
}
