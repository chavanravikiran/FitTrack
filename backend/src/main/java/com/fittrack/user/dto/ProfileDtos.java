package com.fittrack.user.dto;

import com.fittrack.common.validation.ValidTimeZone;
import com.fittrack.user.entity.Gender;
import com.fittrack.user.entity.ThemePreference;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class ProfileDtos {

    private ProfileDtos() {
    }

    public record ProfileResponse(
            Long userId,
            String email,
            String fullName,
            LocalDate dateOfBirth,
            Integer age,
            Gender gender,
            BigDecimal heightCm,
            String timeZone,
            ThemePreference theme,
            boolean hasPhoto,
            boolean onboardingCompleted,
            List<String> roles,
            BigDecimal currentWeightKg,
            BigDecimal targetWeightKg,
            BigDecimal bmi,
            Instant memberSince) {
    }

    public record UpdateProfileRequest(
            @NotBlank @Size(min = 2, max = 100) String fullName,
            @NotNull @Past LocalDate dateOfBirth,
            @NotNull Gender gender,
            @NotNull @DecimalMin("90.0") @DecimalMax("250.0") @Digits(integer = 3, fraction = 1) BigDecimal heightCm,
            @NotBlank @ValidTimeZone String timeZone,
            @NotNull ThemePreference theme) {
    }

    public record ThemeRequest(@NotNull ThemePreference theme) {
    }
}
