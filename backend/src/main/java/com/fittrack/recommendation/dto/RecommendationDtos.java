package com.fittrack.recommendation.dto;

import java.math.BigDecimal;
import java.util.List;

public final class RecommendationDtos {

    private RecommendationDtos() {
    }

    /** Daily numeric targets used across the app (water goal, step goal, nutrition targets). */
    public record DailyTargets(Integer bmrKcal, Integer tdeeKcal, Integer calorieTargetKcal, Integer proteinMinG,
                               Integer proteinMaxG, Integer carbsG, Integer fatG, int waterMl, int waterTrainingDayMl,
                               int stepTarget, double sleepGoalHours) {
    }

    /** One recommendation with a beginner-friendly explanation. */
    public record RecommendationItem(String category, String icon, String title, String value, String explanation) {
    }

    public record RecommendationResponse(BigDecimal bmi, String bmiCategory, DailyTargets targets,
                                         List<RecommendationItem> items, String disclaimer) {
    }
}
