package com.fittrack.assessment.dto;

import com.fittrack.assessment.entity.FitnessGoal.GoalStatus;
import com.fittrack.assessment.entity.FitnessGoal.GoalType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public final class GoalDtos {

    private GoalDtos() {
    }

    public record GoalRequest(
            @NotNull GoalType goalType,
            @Size(max = 120) String title,
            @NotNull @DecimalMin(value = "0.0", inclusive = false) @Digits(integer = 8, fraction = 2) BigDecimal targetValue,
            @Future LocalDate targetDate) {
    }

    public record GoalUpdateRequest(
            @Size(max = 120) String title,
            @NotNull @DecimalMin(value = "0.0", inclusive = false) @Digits(integer = 8, fraction = 2) BigDecimal targetValue,
            @Future LocalDate targetDate) {
    }

    public record GoalStatusRequest(@NotNull GoalStatus status) {
    }

    public record GoalResponse(Long id, GoalType goalType, String title, BigDecimal startValue,
                               BigDecimal targetValue, String unit, LocalDate startDate, LocalDate targetDate,
                               GoalStatus status, Instant achievedAt) {
    }
}
