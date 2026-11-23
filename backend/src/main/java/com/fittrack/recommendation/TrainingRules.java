package com.fittrack.recommendation;

import com.fittrack.assessment.entity.AssessmentEnums.FitnessLevel;
import com.fittrack.assessment.entity.AssessmentEnums.LimitationSeverity;
import com.fittrack.assessment.entity.AssessmentEnums.PrimaryGoal;
import com.fittrack.assessment.entity.Limitation;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

/**
 * Safety-first training rules shared by the recommendation engine and the plan generator.
 * Never uses gender. Beginners train at most 4 days; every plan keeps at least one rest day.
 */
public final class TrainingRules {

    public enum Split { FULL_BODY, UPPER_LOWER, PUSH_PULL_LEGS }

    public record RepScheme(int sets, int repsMin, int repsMax, int restSec, String rationale) {
    }

    private TrainingRules() {
    }

    public static int maxTrainingDays(FitnessLevel level, Integer age) {
        int max = switch (level) {
            case BEGINNER -> 4;
            case INTERMEDIATE -> 5;
            case ADVANCED -> 6;
        };
        if (age != null && age >= 60) {
            max = Math.min(max, 4);
        }
        return max;
    }

    /**
     * Picks training days from the user's available days, spreading them across the week so muscles get
     * recovery time. Always leaves at least one rest day.
     */
    public static List<DayOfWeek> selectTrainingDays(Collection<DayOfWeek> available, int maxDays) {
        List<DayOfWeek> sorted = new ArrayList<>(new TreeSet<>(available));
        int target = Math.min(sorted.size(), Math.min(maxDays, 6));
        if (sorted.size() <= target) {
            return sorted;
        }
        List<DayOfWeek> picked = new ArrayList<>();
        for (int i = 0; i < target; i++) {
            int idx = (int) Math.floor(i * (double) sorted.size() / target);
            picked.add(sorted.get(idx));
        }
        return picked;
    }

    public static Split split(int trainingDays, FitnessLevel level) {
        if (trainingDays <= 3) return Split.FULL_BODY;
        if (trainingDays == 4 || level == FitnessLevel.BEGINNER) return Split.UPPER_LOWER;
        return Split.PUSH_PULL_LEGS;
    }

    public static String splitExplanation(Split split, int days) {
        return switch (split) {
            case FULL_BODY -> "Full-body sessions: with " + days + " training day" + (days == 1 ? "" : "s")
                    + " a week, working every major muscle each session gives the best results.";
            case UPPER_LOWER -> "Upper/lower split: alternating upper-body and lower-body days trains each muscle "
                    + "about twice a week while the other half recovers.";
            case PUSH_PULL_LEGS -> "Push/pull/legs split: with " + days + " days you can focus each session on related "
                    + "muscles and still give each group 48+ hours of recovery.";
        };
    }

    /** Target effort on a 1-10 scale (RPE). 6 = could do about 4 more reps, 8 = about 2 more. */
    public static int targetRpe(FitnessLevel level, Integer age, Collection<Limitation> limitations) {
        int rpe = switch (level) {
            case BEGINNER -> 6;
            case INTERMEDIATE -> 7;
            case ADVANCED -> 8;
        };
        if (age != null && age >= 50) rpe--;
        if (limitations.stream().anyMatch(l -> l.getSeverity() == LimitationSeverity.SEVERE)) rpe--;
        return Math.max(5, rpe);
    }

    public static RepScheme repScheme(PrimaryGoal goal, FitnessLevel level, Integer age) {
        boolean beginner = level == FitnessLevel.BEGINNER;
        int extraRest = age != null && age >= 50 ? 15 : 0;
        return switch (goal) {
            case MUSCLE_GAIN -> new RepScheme(3, 8, 12, 90 + extraRest,
                    "3 sets of 8-12 reps with about 90 seconds rest is a proven range for building muscle.");
            case WEIGHT_LOSS -> new RepScheme(beginner ? 2 : 3, 12, 15, 45 + extraRest,
                    "Moderate weights for 12-15 reps with short rests keep your heart rate up and preserve muscle while you lose fat.");
            case MAINTENANCE -> new RepScheme(beginner ? 2 : 3, 10, 12, 60 + extraRest,
                    "10-12 reps maintain strength and muscle with manageable effort.");
            case GENERAL_FITNESS -> new RepScheme(beginner ? 2 : 3, 10, 15, 60 + extraRest,
                    "A 10-15 rep range builds all-round strength and endurance.");
        };
    }

    public static int exercisesPerSession(int sessionMinutes) {
        // ~10 minutes for warm-up and cool-down, ~7 minutes per exercise including rest.
        return Math.max(3, Math.min(7, (sessionMinutes - 10) / 7));
    }
}
