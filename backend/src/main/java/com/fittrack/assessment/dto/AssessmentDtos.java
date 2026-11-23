package com.fittrack.assessment.dto;

import com.fittrack.assessment.entity.AssessmentEnums.ActivityLevel;
import com.fittrack.assessment.entity.AssessmentEnums.BodyArea;
import com.fittrack.assessment.entity.AssessmentEnums.DietPreference;
import com.fittrack.assessment.entity.AssessmentEnums.FitnessLevel;
import com.fittrack.assessment.entity.AssessmentEnums.LimitationSeverity;
import com.fittrack.assessment.entity.AssessmentEnums.PrimaryGoal;
import com.fittrack.assessment.entity.AssessmentEnums.WorkoutLocation;
import com.fittrack.common.validation.ValidTimeZone;
import com.fittrack.user.entity.Gender;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public final class AssessmentDtos {

    private AssessmentDtos() {
    }

    public record EquipmentDto(String code, String name) {
    }

    public record LimitationDto(@NotNull BodyArea bodyArea, @NotNull LimitationSeverity severity) {
    }

    public record AssessmentRequest(
            @NotNull FitnessLevel fitnessLevel,
            @NotNull PrimaryGoal primaryGoal,
            @NotNull WorkoutLocation workoutLocation,
            @NotEmpty(message = "select at least one workout day") Set<@NotNull DayOfWeek> availableDays,
            @NotNull @Min(10) @Max(180) Integer sessionMinutes,
            @Size(max = 13) Set<@NotBlank String> equipmentCodes,
            @NotNull DietPreference dietPreference,
            @Size(max = 500) String allergies,
            @Size(max = 500) String dietaryRestrictions,
            @Size(max = 1000) String injuryNotes,
            @Size(max = 8) List<@Valid LimitationDto> limitations,
            @NotNull ActivityLevel activityLevel,
            @NotNull @DecimalMin("3.0") @DecimalMax("14.0") @Digits(integer = 2, fraction = 1) BigDecimal sleepHours) {
    }

    public record AssessmentResponse(
            Long id,
            FitnessLevel fitnessLevel,
            PrimaryGoal primaryGoal,
            WorkoutLocation workoutLocation,
            List<DayOfWeek> availableDays,
            int sessionMinutes,
            List<EquipmentDto> equipment,
            DietPreference dietPreference,
            String allergies,
            String dietaryRestrictions,
            String injuryNotes,
            List<LimitationDto> limitations,
            ActivityLevel activityLevel,
            BigDecimal sleepHours,
            boolean current,
            Instant createdAt) {
    }

    public record AssessmentSummary(Long id, FitnessLevel fitnessLevel, PrimaryGoal primaryGoal,
                                    WorkoutLocation workoutLocation, int daysPerWeek, boolean current,
                                    Instant createdAt) {
    }

    /** Everything collected by the onboarding wizard, saved atomically. */
    public record OnboardingRequest(
            @NotBlank @Size(min = 2, max = 100) String fullName,
            @NotNull @Past LocalDate dateOfBirth,
            @NotNull Gender gender,
            @NotNull @DecimalMin("90.0") @DecimalMax("250.0") @Digits(integer = 3, fraction = 1) BigDecimal heightCm,
            @NotNull @DecimalMin("20.0") @DecimalMax("400.0") @Digits(integer = 3, fraction = 2) BigDecimal currentWeightKg,
            @NotNull @DecimalMin("30.0") @DecimalMax("300.0") @Digits(integer = 3, fraction = 2) BigDecimal targetWeightKg,
            @NotBlank @ValidTimeZone String timeZone,
            @NotNull @Valid AssessmentRequest assessment) {
    }
}
