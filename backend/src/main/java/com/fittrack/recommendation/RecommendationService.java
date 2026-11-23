package com.fittrack.recommendation;

import com.fittrack.activity.repository.DailyActivityRecordRepository;
import com.fittrack.assessment.entity.AssessmentEnums.ActivityLevel;
import com.fittrack.assessment.entity.AssessmentEnums.PrimaryGoal;
import com.fittrack.assessment.entity.FitnessAssessment;
import com.fittrack.assessment.entity.FitnessGoal.GoalStatus;
import com.fittrack.assessment.entity.FitnessGoal.GoalType;
import com.fittrack.assessment.repository.FitnessGoalRepository;
import com.fittrack.common.util.DayMask;
import com.fittrack.recommendation.calculator.BodyMetrics;
import com.fittrack.recommendation.dto.RecommendationDtos.DailyTargets;
import com.fittrack.recommendation.dto.RecommendationDtos.RecommendationItem;
import com.fittrack.recommendation.dto.RecommendationDtos.RecommendationResponse;
import com.fittrack.security.CurrentUserService;
import com.fittrack.user.entity.UserProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Rule-based, explainable recommendations. Safety rules:
 * calorie deficit at most 500 kcal or 20% of TDEE and never below BMR; surplus at most 300 kcal;
 * gradual step targets; rest days always included; gender only affects the BMR estimate.
 */
@Service
@RequiredArgsConstructor
public class RecommendationService {

    public static final String DISCLAIMER = "These are educational estimates based on common formulas, not medical "
            + "advice. Individual needs vary - adjust based on how you feel and consult a professional if you have a "
            + "medical condition, are pregnant or have an injury.";

    private final FitnessContextService contextService;
    private final FitnessGoalRepository goalRepository;
    private final DailyActivityRecordRepository activityRepository;
    private final CurrentUserService currentUser;

    @Transactional(readOnly = true)
    public RecommendationResponse forCurrentUser() {
        FitnessContext ctx = contextService.load(currentUser.userId());
        DailyTargets t = targets(ctx);
        List<RecommendationItem> items = new ArrayList<>();
        FitnessAssessment a = ctx.assessment();

        if (a != null) {
            int maxDays = TrainingRules.maxTrainingDays(a.getFitnessLevel(), ctx.age());
            int available = Integer.bitCount(a.getAvailableDays());
            int days = TrainingRules.selectTrainingDays(DayMask.fromMask(a.getAvailableDays()), maxDays).size();
            var split = TrainingRules.split(days, a.getFitnessLevel());
            var scheme = TrainingRules.repScheme(a.getPrimaryGoal(), a.getFitnessLevel(), ctx.age());
            int rpe = TrainingRules.targetRpe(a.getFitnessLevel(), ctx.age(), a.getLimitations());

            items.add(new RecommendationItem("WORKOUT", "calendar_month", "Training schedule",
                    days + " days per week · " + (7 - days) + " rest days",
                    (available > days ? "You have " + available + " days available; " + days
                            + " training days leave enough recovery for your level. " : "")
                            + "Muscles grow and adapt on rest days, so they are part of the plan."));
            items.add(new RecommendationItem("WORKOUT", "view_week", "Workout split", label(split.name()),
                    TrainingRules.splitExplanation(split, days)));
            items.add(new RecommendationItem("WORKOUT", "repeat", "Sets and reps",
                    scheme.sets() + " sets × " + scheme.repsMin() + "-" + scheme.repsMax() + " reps · rest " + scheme.restSec() + "s",
                    scheme.rationale()));
            items.add(new RecommendationItem("WORKOUT", "speed", "Intensity", "Effort " + rpe + "/10",
                    "Finish each set feeling you could do about " + (10 - rpe) + " more reps. Increase weight or reps "
                            + "only when the last set feels easy for two sessions in a row."));
            if (!a.getLimitations().isEmpty()) {
                items.add(new RecommendationItem("SAFETY", "healing", "Limitations respected",
                        a.getLimitations().size() + " area(s) protected",
                        "Exercises that load your declared areas are swapped for gentler alternatives. Stop any "
                                + "movement that causes pain."));
            }
        }

        items.add(new RecommendationItem("ACTIVITY", "directions_walk", "Daily steps", String.format("%,d steps", t.stepTarget()),
                "A gradual target based on your current activity. We raise it by about 1,000 steps a week up to 10,000 "
                        + "as it becomes easy."));
        items.add(new RecommendationItem("HYDRATION", "water_drop", "Water",
                String.format("%,d ml (%,d ml on workout days)", t.waterMl(), t.waterTrainingDayMl()),
                "About 35 ml per kg of body weight, plus extra on training days and in hot weather. Your thirst and "
                        + "urine colour (pale yellow) are good guides."));
        if (t.calorieTargetKcal() != null) {
            items.add(new RecommendationItem("NUTRITION", "local_fire_department", "Daily energy",
                    String.format("~%,d kcal", t.calorieTargetKcal()), calorieExplanation(a, t)));
            items.add(new RecommendationItem("NUTRITION", "egg", "Protein",
                    t.proteinMinG() + "-" + t.proteinMaxG() + " g per day",
                    "Spread protein across meals - dal, paneer, curd, eggs, chicken, soya or tofu all count. It helps you "
                            + "recover and keep muscle."));
        }
        items.add(new RecommendationItem("RECOVERY", "bedtime", "Sleep", t.sleepGoalHours() + " hours",
                "7-9 hours of sleep improves recovery, appetite control and energy for workouts."));

        BigDecimal bmi = ctx.weightKg() != null && ctx.profile().getHeightCm() != null
                ? BodyMetrics.bmi(ctx.weightKg(), ctx.profile().getHeightCm()) : null;
        return new RecommendationResponse(bmi, bmi == null ? null : BodyMetrics.bmiCategory(bmi), t, items, DISCLAIMER);
    }

    @Transactional(readOnly = true)
    public DailyTargets targetsFor(Long userId) {
        return targets(contextService.load(userId));
    }

    DailyTargets targets(FitnessContext ctx) {
        UserProfile p = ctx.profile();
        FitnessAssessment a = ctx.assessment();
        double weight = ctx.weightOr(70);

        Integer bmr = null, tdee = null, calories = null, proteinMin = null, proteinMax = null, carbs = null, fat = null;
        if (ctx.weightKg() != null && p.getHeightCm() != null && ctx.age() != null) {
            bmr = BodyMetrics.bmr(weight, p.getHeightCm().doubleValue(), ctx.age(), p.getGender());
            ActivityLevel level = a == null ? ActivityLevel.LIGHTLY_ACTIVE : a.getActivityLevel();
            tdee = BodyMetrics.tdee(bmr, level);
            PrimaryGoal goal = a == null ? PrimaryGoal.MAINTENANCE : a.getPrimaryGoal();
            int target = switch (goal) {
                case WEIGHT_LOSS -> Math.max(bmr, tdee - Math.min(500, (int) Math.round(tdee * 0.20)));
                case MUSCLE_GAIN -> tdee + Math.min(300, (int) Math.round(tdee * 0.10));
                default -> tdee;
            };
            calories = BodyMetrics.roundTo(target, 50);
            boolean higherProtein = goal == PrimaryGoal.WEIGHT_LOSS || goal == PrimaryGoal.MUSCLE_GAIN;
            proteinMin = (int) Math.round(weight * (higherProtein ? 1.2 : 1.0));
            proteinMax = (int) Math.round(weight * (higherProtein ? 1.6 : 1.4));
            fat = (int) Math.round(calories * 0.275 / 9);
            int proteinMid = (proteinMin + proteinMax) / 2;
            carbs = Math.max(0, (int) Math.round((calories - proteinMid * 4 - fat * 9) / 4.0));
        }

        int water = Math.min(4000, BodyMetrics.roundTo((int) Math.round(weight * 35), 250));
        Integer waterGoal = activeGoal(ctx.userId(), GoalType.DAILY_WATER);
        if (waterGoal != null) water = waterGoal;

        Integer stepGoal = activeGoal(ctx.userId(), GoalType.DAILY_STEPS);
        int steps = stepGoal != null ? stepGoal : defaultSteps(ctx, a);

        Integer sleepGoal = activeGoal(ctx.userId(), GoalType.SLEEP_HOURS);
        double sleep = sleepGoal != null ? sleepGoal : 8.0;

        return new DailyTargets(bmr, tdee, calories, proteinMin, proteinMax, carbs, fat, water, water + 500, steps, sleep);
    }

    private int defaultSteps(FitnessContext ctx, FitnessAssessment a) {
        double avg = activityRepository.averageSteps(ctx.userId(), ctx.today().minusDays(7), ctx.today().minusDays(1));
        if (avg > 0) {
            return Math.min(10_000, Math.max(5_000, BodyMetrics.roundTo((int) avg + 1_000, 500)));
        }
        ActivityLevel level = a == null ? ActivityLevel.LIGHTLY_ACTIVE : a.getActivityLevel();
        return switch (level) {
            case SEDENTARY -> 5_000;
            case LIGHTLY_ACTIVE -> 6_500;
            case MODERATELY_ACTIVE -> 8_000;
            case VERY_ACTIVE -> 10_000;
        };
    }

    private Integer activeGoal(Long userId, GoalType type) {
        return goalRepository.findByUserIdAndGoalTypeAndStatus(userId, type, GoalStatus.ACTIVE)
                .map(g -> g.getTargetValue().intValue()).orElse(null);
    }

    private static String calorieExplanation(FitnessAssessment a, DailyTargets t) {
        PrimaryGoal goal = a == null ? PrimaryGoal.MAINTENANCE : a.getPrimaryGoal();
        String base = String.format("Your body uses roughly %,d kcal a day (resting %,d kcal). ", t.tdeeKcal(), t.bmrKcal());
        return base + switch (goal) {
            case WEIGHT_LOSS -> "A modest deficit supports steady fat loss of up to about 0.5 kg per week. We never go "
                    + "below your resting needs - crash diets backfire.";
            case MUSCLE_GAIN -> "A small surplus provides energy to build muscle while limiting fat gain.";
            default -> "Eating around this amount keeps your weight stable.";
        };
    }

    private static String label(String enumName) {
        return switch (enumName) {
            case "FULL_BODY" -> "Full body";
            case "UPPER_LOWER" -> "Upper / lower";
            default -> "Push / pull / legs";
        };
    }
}
