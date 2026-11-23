package com.fittrack.assessment.entity;

/** Enumerations captured by the fitness assessment. Stored as VARCHAR with matching DB CHECK constraints. */
public final class AssessmentEnums {

    private AssessmentEnums() {
    }

    public enum FitnessLevel { BEGINNER, INTERMEDIATE, ADVANCED }

    public enum PrimaryGoal { WEIGHT_LOSS, MUSCLE_GAIN, MAINTENANCE, GENERAL_FITNESS }

    public enum WorkoutLocation { HOME, GYM }

    public enum DietPreference { VEGETARIAN, NON_VEGETARIAN, VEGAN, EGGETARIAN }

    /** Multipliers follow the commonly used Harris-Benedict/Mifflin activity factors. */
    public enum ActivityLevel {
        SEDENTARY(1.2), LIGHTLY_ACTIVE(1.375), MODERATELY_ACTIVE(1.55), VERY_ACTIVE(1.725);

        private final double multiplier;

        ActivityLevel(double multiplier) {
            this.multiplier = multiplier;
        }

        public double multiplier() {
            return multiplier;
        }
    }

    public enum BodyArea { KNEE, LOWER_BACK, SHOULDER, WRIST, ANKLE, NECK, HIP, ELBOW }

    public enum LimitationSeverity { MILD, MODERATE, SEVERE }
}
