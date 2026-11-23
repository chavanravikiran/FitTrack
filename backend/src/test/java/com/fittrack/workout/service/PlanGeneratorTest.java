package com.fittrack.workout.service;

import com.fittrack.assessment.entity.AssessmentEnums.ActivityLevel;
import com.fittrack.assessment.entity.AssessmentEnums.BodyArea;
import com.fittrack.assessment.entity.AssessmentEnums.DietPreference;
import com.fittrack.assessment.entity.AssessmentEnums.FitnessLevel;
import com.fittrack.assessment.entity.AssessmentEnums.LimitationSeverity;
import com.fittrack.assessment.entity.AssessmentEnums.PrimaryGoal;
import com.fittrack.assessment.entity.AssessmentEnums.WorkoutLocation;
import com.fittrack.assessment.entity.Equipment;
import com.fittrack.assessment.entity.FitnessAssessment;
import com.fittrack.assessment.entity.Limitation;
import com.fittrack.common.util.DayMask;
import com.fittrack.exercise.entity.Exercise;
import com.fittrack.exercise.entity.MuscleGroup;
import com.fittrack.workout.entity.WorkoutExercise;
import com.fittrack.workout.entity.WorkoutPlan;
import com.fittrack.workout.entity.WorkoutPlanDay;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PlanGeneratorTest {

    private final PlanGenerator generator = new PlanGenerator();
    private final Map<String, MuscleGroup> groups = new java.util.HashMap<>();
    private final Map<String, Equipment> equipment = new java.util.HashMap<>();
    private List<Exercise> catalog;
    private long ids = 1;

    @BeforeEach
    void setUp() {
        catalog = new ArrayList<>();
        for (String g : List.of("CHEST", "BACK", "SHOULDERS", "BICEPS", "TRICEPS", "LEGS", "ABS", "CARDIO")) {
            MuscleGroup m = new MuscleGroup();
            m.setCode(g);
            m.setName(g.charAt(0) + g.substring(1).toLowerCase());
            groups.put(g, m);
        }
        for (String e : List.of("NONE", "YOGA_MAT", "DUMBBELLS", "BARBELL", "MACHINES")) {
            Equipment q = new Equipment();
            q.setCode(e);
            equipment.put(e, q);
        }
        add("Push-up", "CHEST", Exercise.Difficulty.BEGINNER, Exercise.Location.BOTH, Set.of("NONE"), Set.of(BodyArea.WRIST));
        add("Dumbbell Floor Press", "CHEST", Exercise.Difficulty.BEGINNER, Exercise.Location.BOTH, Set.of("DUMBBELLS"), Set.of());
        add("Barbell Bench Press", "CHEST", Exercise.Difficulty.INTERMEDIATE, Exercise.Location.GYM, Set.of("BARBELL"), Set.of());
        add("Dumbbell Row", "BACK", Exercise.Difficulty.BEGINNER, Exercise.Location.BOTH, Set.of("DUMBBELLS"), Set.of());
        add("Superman", "BACK", Exercise.Difficulty.BEGINNER, Exercise.Location.BOTH, Set.of("YOGA_MAT"), Set.of(BodyArea.LOWER_BACK));
        add("Lat Pulldown", "BACK", Exercise.Difficulty.BEGINNER, Exercise.Location.GYM, Set.of("MACHINES"), Set.of());
        add("Shoulder Press", "SHOULDERS", Exercise.Difficulty.BEGINNER, Exercise.Location.BOTH, Set.of("DUMBBELLS"), Set.of(BodyArea.SHOULDER));
        add("Curl", "BICEPS", Exercise.Difficulty.BEGINNER, Exercise.Location.BOTH, Set.of("DUMBBELLS"), Set.of());
        add("Bench Dip", "TRICEPS", Exercise.Difficulty.INTERMEDIATE, Exercise.Location.BOTH, Set.of("NONE"), Set.of(BodyArea.WRIST));
        add("Squat", "LEGS", Exercise.Difficulty.BEGINNER, Exercise.Location.BOTH, Set.of("NONE"), Set.of(BodyArea.KNEE));
        add("Glute Bridge", "LEGS", Exercise.Difficulty.BEGINNER, Exercise.Location.BOTH, Set.of("YOGA_MAT"), Set.of());
        add("Leg Press", "LEGS", Exercise.Difficulty.BEGINNER, Exercise.Location.GYM, Set.of("MACHINES"), Set.of(BodyArea.KNEE));
        add("Dead Bug", "ABS", Exercise.Difficulty.BEGINNER, Exercise.Location.BOTH, Set.of("YOGA_MAT"), Set.of());
        Exercise walk = add("Brisk Walk", "CARDIO", Exercise.Difficulty.BEGINNER, Exercise.Location.BOTH, Set.of("NONE"), Set.of());
        walk.setDefaultReps(null);
        walk.setDefaultSets(1);
        walk.setDefaultDurationSec(600);
    }

    @Test
    void beginnerWithSixDaysTrainsAtMostFourAndKeepsRestDays() {
        FitnessAssessment a = assessment(FitnessLevel.BEGINNER, PrimaryGoal.WEIGHT_LOSS, WorkoutLocation.HOME,
                EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY),
                Set.of("DUMBBELLS"), Set.of());

        WorkoutPlan plan = generator.generate(new PlanGenerator.Input(a, 30, LocalDate.of(2026, 1, 5)), catalog);

        assertThat(plan.getDays()).hasSize(7);
        long training = plan.getDays().stream().filter(d -> !d.isRestDay()).count();
        assertThat(training).isEqualTo(4);
        assertThat(plan.getSplitType()).isEqualTo(WorkoutPlan.SplitType.UPPER_LOWER);
        assertThat(plan.getNotes()).contains("recovery");
        // Weight loss keeps a cardio finisher on every training day
        plan.getDays().stream().filter(d -> !d.isRestDay()).forEach(d ->
                assertThat(d.getExercises()).anyMatch(e -> e.getExercise().getPrimaryMuscleGroup().getCode().equals("CARDIO")));
    }

    @Test
    void homeUserNeverGetsGymOnlyOrMissingEquipmentExercises() {
        FitnessAssessment a = assessment(FitnessLevel.BEGINNER, PrimaryGoal.GENERAL_FITNESS, WorkoutLocation.HOME,
                EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY), Set.of(), Set.of());

        List<String> names = exerciseNames(generator.generate(new PlanGenerator.Input(a, 30, LocalDate.now()), catalog));

        assertThat(names).doesNotContain("Barbell Bench Press", "Lat Pulldown", "Leg Press", "Dumbbell Row", "Curl");
        assertThat(names).isNotEmpty();
    }

    @Test
    void declaredLimitationsExcludeContraindicatedExercises() {
        FitnessAssessment a = assessment(FitnessLevel.BEGINNER, PrimaryGoal.MUSCLE_GAIN, WorkoutLocation.GYM,
                EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY), Set.of(),
                Set.of(new Limitation(BodyArea.KNEE, LimitationSeverity.MODERATE), new Limitation(BodyArea.WRIST, LimitationSeverity.MILD)));

        WorkoutPlan plan = generator.generate(new PlanGenerator.Input(a, 30, LocalDate.now()), catalog);
        List<String> names = exerciseNames(plan);

        assertThat(names).doesNotContain("Squat", "Leg Press", "Push-up", "Bench Dip");
        assertThat(names).contains("Glute Bridge");
        assertThat(plan.getNotes()).contains("limitations");
    }

    @Test
    void beginnersNeverGetIntermediateExercisesAndRepRangesFollowGoal() {
        FitnessAssessment a = assessment(FitnessLevel.BEGINNER, PrimaryGoal.MUSCLE_GAIN, WorkoutLocation.GYM,
                EnumSet.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY), Set.of(), Set.of());

        WorkoutPlan plan = generator.generate(new PlanGenerator.Input(a, 55, LocalDate.now()), catalog);
        List<WorkoutExercise> all = plan.getDays().stream().flatMap(d -> d.getExercises().stream()).toList();

        assertThat(all).noneMatch(e -> e.getExercise().getDifficulty() != Exercise.Difficulty.BEGINNER);
        assertThat(all).filteredOn(e -> e.getRepsMin() != null).allSatisfy(e -> {
            assertThat(e.getRepsMin()).isEqualTo(8);
            assertThat(e.getRepsMax()).isEqualTo(12);
            assertThat(e.getRestSec()).isEqualTo(105); // 90 + 15 for age 50+
            assertThat(e.getTargetRpe()).isEqualTo(5); // beginner 6, minus 1 for age 50+
        });
    }

    @Test
    void sameInputsProduceTheSamePlan() {
        FitnessAssessment a = assessment(FitnessLevel.BEGINNER, PrimaryGoal.MAINTENANCE, WorkoutLocation.GYM,
                EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY), Set.of(), Set.of());
        assertThat(exerciseNames(generator.generate(new PlanGenerator.Input(a, 30, LocalDate.now()), catalog)))
                .isEqualTo(exerciseNames(generator.generate(new PlanGenerator.Input(a, 30, LocalDate.now()), catalog)));
    }

    private List<String> exerciseNames(WorkoutPlan plan) {
        return plan.getDays().stream().flatMap((WorkoutPlanDay d) -> d.getExercises().stream())
                .map(e -> e.getExercise().getName()).toList();
    }

    private Exercise add(String name, String group, Exercise.Difficulty difficulty, Exercise.Location location,
                         Set<String> eq, Set<BodyArea> avoid) {
        Exercise e = new Exercise();
        e.setId(ids++);
        e.setName(name);
        e.setSlug(name.toLowerCase().replace(' ', '-'));
        e.setPrimaryMuscleGroup(groups.get(group));
        e.setDifficulty(difficulty);
        e.setLocation(location);
        e.setDefaultSets(3);
        e.setDefaultReps(10);
        e.setDefaultRestSec(60);
        e.setMetValue(BigDecimal.valueOf(4));
        eq.forEach(code -> e.getEquipment().add(equipment.get(code)));
        e.getContraindications().addAll(avoid);
        catalog.add(e);
        return e;
    }

    private FitnessAssessment assessment(FitnessLevel level, PrimaryGoal goal, WorkoutLocation loc, Set<DayOfWeek> days,
                                         Set<String> owned, Set<Limitation> limitations) {
        FitnessAssessment a = new FitnessAssessment();
        a.setFitnessLevel(level);
        a.setPrimaryGoal(goal);
        a.setWorkoutLocation(loc);
        a.setAvailableDays(DayMask.toMask(days));
        a.setSessionMinutes(45);
        a.setDietPreference(DietPreference.VEGETARIAN);
        a.setActivityLevel(ActivityLevel.LIGHTLY_ACTIVE);
        a.setSleepHours(BigDecimal.valueOf(7));
        owned.forEach(code -> a.getEquipment().add(equipment.get(code)));
        a.getLimitations().addAll(limitations);
        return a;
    }
}
