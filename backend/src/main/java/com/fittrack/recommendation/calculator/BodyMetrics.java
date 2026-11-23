package com.fittrack.recommendation.calculator;

import com.fittrack.assessment.entity.AssessmentEnums.ActivityLevel;
import com.fittrack.user.entity.Gender;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Pure body-metric helpers. All values are population-level estimates, not medical assessments.
 */
public final class BodyMetrics {

    /** Lower bound of the WHO "healthy" BMI range. */
    public static final BigDecimal MIN_HEALTHY_BMI = new BigDecimal("18.5");

    private BodyMetrics() {
    }

    public static BigDecimal bmi(BigDecimal weightKg, BigDecimal heightCm) {
        BigDecimal heightM = heightCm.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        return weightKg.divide(heightM.multiply(heightM), 1, RoundingMode.HALF_UP);
    }

    /** Smallest weight (kg) that keeps BMI at or above 18.5 for the given height. */
    public static BigDecimal minHealthyWeight(BigDecimal heightCm) {
        BigDecimal heightM = heightCm.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        return MIN_HEALTHY_BMI.multiply(heightM).multiply(heightM).setScale(1, RoundingMode.UP);
    }

    public static String bmiCategory(BigDecimal bmi) {
        double b = bmi.doubleValue();
        if (b < 18.5) return "Below the healthy range";
        if (b < 25) return "Healthy range";
        if (b < 30) return "Above the healthy range";
        return "Well above the healthy range";
    }

    /**
     * Mifflin-St Jeor resting energy. Gender is used only here (physiology), never for exercise choice.
     * "Prefer not to say" uses the midpoint of the two equations' constants (+5 / -161 -> -78).
     */
    public static int bmr(double weightKg, double heightCm, int age, Gender gender) {
        double base = 10 * weightKg + 6.25 * heightCm - 5 * age;
        double constant = switch (gender == null ? Gender.PREFER_NOT_TO_SAY : gender) {
            case MALE -> 5;
            case FEMALE -> -161;
            case PREFER_NOT_TO_SAY -> -78;
        };
        return (int) Math.round(base + constant);
    }

    public static int tdee(int bmr, ActivityLevel level) {
        return (int) Math.round(bmr * level.multiplier());
    }

    public static int roundTo(int value, int step) {
        return Math.round((float) value / step) * step;
    }
}
