package com.fittrack.workout.service;

import com.fittrack.assessment.entity.AssessmentEnums.BodyArea;
import com.fittrack.assessment.entity.AssessmentEnums.FitnessLevel;
import com.fittrack.assessment.entity.AssessmentEnums.PrimaryGoal;
import com.fittrack.assessment.entity.AssessmentEnums.WorkoutLocation;
import com.fittrack.assessment.entity.Equipment;
import com.fittrack.assessment.entity.FitnessAssessment;
import com.fittrack.assessment.entity.Limitation;
import com.fittrack.common.util.DayMask;
import com.fittrack.exercise.entity.Exercise;
import com.fittrack.recommendation.TrainingRules;
import com.fittrack.recommendation.TrainingRules.RepScheme;
import com.fittrack.recommendation.TrainingRules.Split;
import com.fittrack.workout.entity.WorkoutExercise;
import com.fittrack.workout.entity.WorkoutPlan;
import com.fittrack.workout.entity.WorkoutPlanDay;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Builds a weekly plan from the fitness assessment. Deterministic for the same inputs, so the user sees a
 * stable plan; regenerating after a re-assessment adapts it.
 */
@Component
public class PlanGenerator {

    private static final Set<String> ALWAYS_AVAILABLE = Set.of("NONE", "YOGA_MAT");

    record DayTemplate(String title, List<String> slots) {
    }

    public record Input(FitnessAssessment assessment, Integer age, LocalDate startDate) {
    }

    public WorkoutPlan generate(Input in, List<Exercise> catalog) {
        FitnessAssessment a = in.assessment();
        FitnessLevel level = a.getFitnessLevel();
        PrimaryGoal goal = a.getPrimaryGoal();
        Set<Limitation> limitations = a.getLimitations();

        List<DayOfWeek> trainingDays = TrainingRules.selectTrainingDays(
                DayMask.fromMask(a.getAvailableDays()), TrainingRules.maxTrainingDays(level, in.age()));
        Split split = TrainingRules.split(trainingDays.size(), level);
        RepScheme scheme = TrainingRules.repScheme(goal, level, in.age());
        int rpe = TrainingRules.targetRpe(level, in.age(), limitations);
        int perSession = TrainingRules.exercisesPerSession(a.getSessionMinutes());
        boolean cardioFocus = goal == PrimaryGoal.WEIGHT_LOSS || goal == PrimaryGoal.GENERAL_FITNESS;

        List<Exercise> eligible = catalog.stream().filter(e -> isEligible(e, a)).toList();
        List<DayTemplate> templates = templates(split, trainingDays.size());

        WorkoutPlan plan = new WorkoutPlan();
        plan.setName("%s %s (%d day%s/week)".formatted(capitalize(level.name()), splitName(split),
                trainingDays.size(), trainingDays.size() == 1 ? "" : "s"));
        plan.setSplitType(WorkoutPlan.SplitType.valueOf(split.name()));
        plan.setStartDate(in.startDate());
        plan.setNotes(truncate(String.join("\n", explanations(a, trainingDays, split, scheme, rpe, in.age())), 1000));

        Map<Long, Integer> usage = new HashMap<>();
        int templateIdx = 0;
        for (DayOfWeek dow : DayOfWeek.values()) {
            WorkoutPlanDay day = new WorkoutPlanDay();
            day.setDayOfWeek(dow.getValue());
            day.setSortOrder(dow.getValue());
            if (!trainingDays.contains(dow)) {
                day.setRestDay(true);
                day.setTitle("Rest & recovery");
                day.setFocus("Light walk or gentle stretching. Your body adapts and gets stronger on rest days.");
                plan.addDay(day);
                continue;
            }
            DayTemplate template = templates.get(templateIdx % templates.size());
            List<String> slots = slotsFor(template, perSession, cardioFocus);
            List<Exercise> chosen = pick(slots, eligible, usage, templateIdx, level);

            day.setTitle(template.title());
            Set<String> groups = chosen.stream()
                    .map(e -> e.getPrimaryMuscleGroup().getName())
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            day.setFocus(truncate("Warm up 5 min · " + String.join(", ", groups) + " · cool down with stretches", 255));
            int order = 1;
            for (Exercise ex : chosen) {
                day.addExercise(prescribe(ex, scheme, rpe, goal, order++, reason(ex, a)));
            }
            plan.addDay(day);
            templateIdx++;
        }
        return plan;
    }

    // ---------------------------------------------------------------------------------------------

    boolean isEligible(Exercise e, FitnessAssessment a) {
        if (!e.isActive()) return false;
        if (e.getDifficulty().rank() > levelRank(a.getFitnessLevel())) return false;
        if (a.getWorkoutLocation() == WorkoutLocation.HOME && e.getLocation() == Exercise.Location.GYM) return false;
        Set<BodyArea> limited = a.getLimitations().stream().map(Limitation::getBodyArea).collect(Collectors.toSet());
        if (e.getContraindications().stream().anyMatch(limited::contains)) return false;
        if (a.getWorkoutLocation() == WorkoutLocation.GYM) return true; // gyms have the standard equipment
        Set<String> owned = a.getEquipment().stream().map(Equipment::getCode).collect(Collectors.toSet());
        return e.getEquipment().stream().map(Equipment::getCode)
                .allMatch(code -> ALWAYS_AVAILABLE.contains(code) || owned.contains(code));
    }

    private List<Exercise> pick(List<String> slots, List<Exercise> eligible, Map<Long, Integer> usage, int dayIdx,
                                FitnessLevel level) {
        List<Exercise> chosen = new ArrayList<>();
        Set<Long> today = new HashSet<>();
        Map<String, Integer> slotRepeat = new HashMap<>();
        for (String slot : slots) {
            int repeat = slotRepeat.merge(slot, 1, Integer::sum) - 1;
            List<Exercise> candidates = eligible.stream()
                    .filter(e -> e.getPrimaryMuscleGroup().getCode().equals(slot))
                    .filter(e -> !today.contains(e.getId()))
                    .sorted(Comparator
                            .comparingInt((Exercise e) -> usage.getOrDefault(e.getId(), 0))
                            .thenComparingInt(e -> Math.abs(e.getDifficulty().rank() - levelRank(level)))
                            .thenComparing(Exercise::getName))
                    .toList();
            if (candidates.isEmpty()) continue;
            // Rotate among equally good options so different days get variety.
            int poolSize = Math.min(3, candidates.size());
            Exercise ex = candidates.get((dayIdx + repeat) % poolSize);
            chosen.add(ex);
            today.add(ex.getId());
            usage.merge(ex.getId(), 1, Integer::sum);
        }
        return chosen;
    }

    WorkoutExercise prescribe(Exercise ex, RepScheme scheme, int rpe, PrimaryGoal goal, int order, String note) {
        WorkoutExercise we = new WorkoutExercise();
        we.setExercise(ex);
        we.setSortOrder(order);
        we.setTargetRpe(rpe);
        we.setNotes(truncate(note, 255));
        String group = ex.getPrimaryMuscleGroup().getCode();
        if (ex.isDurationBased()) {
            int duration = ex.getDefaultDurationSec();
            if ("CARDIO".equals(group) && ex.getDefaultSets() != null && ex.getDefaultSets() == 1 && goal == PrimaryGoal.WEIGHT_LOSS) {
                duration = Math.min(1200, (int) Math.round(duration * 1.5));
            }
            we.setSets(ex.getDefaultSets() == null ? 1 : ex.getDefaultSets());
            we.setDurationSec(duration);
            we.setRestSec(ex.getDefaultRestSec() == null ? 30 : ex.getDefaultRestSec());
            return we;
        }
        int defaultReps = ex.getDefaultReps() == null ? scheme.repsMin() : ex.getDefaultReps();
        if (defaultReps < scheme.repsMin()) {
            // Harder bodyweight moves (pull-ups, pike push-ups) use a lower, achievable range.
            we.setRepsMin(Math.max(3, defaultReps - 2));
            we.setRepsMax(defaultReps + 2);
        } else {
            we.setRepsMin(scheme.repsMin());
            we.setRepsMax(scheme.repsMax());
        }
        we.setSets(scheme.sets());
        we.setRestSec(scheme.restSec());
        return we;
    }

    private String reason(Exercise ex, FitnessAssessment a) {
        List<String> parts = new ArrayList<>();
        List<String> gear = ex.getEquipment().stream().map(Equipment::getCode)
                .filter(c -> !ALWAYS_AVAILABLE.contains(c)).map(c -> c.toLowerCase(Locale.ROOT).replace('_', ' ')).toList();
        parts.add(gear.isEmpty() ? "No equipment needed" : "Uses your " + String.join(" + ", gear));
        if (ex.getDifficulty() == Exercise.Difficulty.BEGINNER && a.getFitnessLevel() == FitnessLevel.BEGINNER) {
            parts.add("beginner-friendly");
        }
        if (!a.getLimitations().isEmpty()) {
            String areas = a.getLimitations().stream().map(l -> l.getBodyArea().name().toLowerCase(Locale.ROOT).replace('_', ' '))
                    .sorted().collect(Collectors.joining(", "));
            parts.add("chosen to avoid stressing your " + areas);
        }
        return String.join(" · ", parts);
    }

    private static List<String> slotsFor(DayTemplate t, int perSession, boolean cardioFocus) {
        List<String> slots = new ArrayList<>(t.slots());
        if (cardioFocus && !slots.contains("CARDIO")) {
            slots.add("CARDIO");
        }
        if (slots.size() <= perSession) return slots;
        // Keep the first strength slots, but always keep a cardio finisher when cardio is a focus.
        List<String> trimmed = new ArrayList<>(slots.subList(0, perSession));
        if (cardioFocus && !trimmed.contains("CARDIO")) {
            trimmed.set(trimmed.size() - 1, "CARDIO");
        }
        return trimmed;
    }

    private static List<DayTemplate> templates(Split split, int days) {
        return switch (split) {
            case FULL_BODY -> List.of(
                    new DayTemplate("Full Body A", List.of("LEGS", "CHEST", "BACK", "SHOULDERS", "ABS", "BICEPS", "TRICEPS")),
                    new DayTemplate("Full Body B", List.of("LEGS", "BACK", "CHEST", "ABS", "SHOULDERS", "TRICEPS", "BICEPS")),
                    new DayTemplate("Full Body C", List.of("LEGS", "SHOULDERS", "BACK", "CHEST", "ABS", "BICEPS", "TRICEPS")));
            case UPPER_LOWER -> List.of(
                    new DayTemplate("Upper Body", List.of("CHEST", "BACK", "SHOULDERS", "BICEPS", "TRICEPS", "ABS", "BACK")),
                    new DayTemplate("Lower Body & Core", List.of("LEGS", "LEGS", "LEGS", "ABS", "ABS", "LEGS", "CARDIO")));
            case PUSH_PULL_LEGS -> List.of(
                    new DayTemplate("Push (chest, shoulders, triceps)", List.of("CHEST", "CHEST", "SHOULDERS", "TRICEPS", "SHOULDERS", "TRICEPS", "ABS")),
                    new DayTemplate("Pull (back, biceps)", List.of("BACK", "BACK", "BICEPS", "BACK", "BICEPS", "ABS", "CARDIO")),
                    new DayTemplate("Legs & Core", List.of("LEGS", "LEGS", "LEGS", "ABS", "LEGS", "ABS", "CARDIO")));
        };
    }

    private static List<String> explanations(FitnessAssessment a, List<DayOfWeek> days, Split split, RepScheme scheme,
                                             int rpe, Integer age) {
        List<String> lines = new ArrayList<>();
        String dayNames = days.stream().map(d -> d.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)).collect(Collectors.joining(", "));
        int available = Integer.bitCount(a.getAvailableDays());
        lines.add("Training on " + dayNames + (available > days.size()
                ? " - spread out from your " + available + " available days so you always have recovery time." : "."));
        lines.add(TrainingRules.splitExplanation(split, days.size()));
        lines.add(scheme.rationale());
        lines.add("Target effort " + rpe + "/10: end each set feeling you could do about " + (10 - rpe) + " more reps.");
        if (!a.getLimitations().isEmpty()) {
            lines.add("Exercises that load your declared limitations were left out. Stop any movement that causes pain.");
        }
        if (age != null && age >= 50) {
            lines.add("Slightly longer rests and a gentler effort target support joint health.");
        }
        return lines;
    }

    private static int levelRank(FitnessLevel l) {
        return l.ordinal();
    }

    private static String splitName(Split s) {
        return switch (s) {
            case FULL_BODY -> "Full Body";
            case UPPER_LOWER -> "Upper/Lower";
            case PUSH_PULL_LEGS -> "Push/Pull/Legs";
        };
    }

    private static String capitalize(String s) {
        return s.charAt(0) + s.substring(1).toLowerCase(Locale.ROOT);
    }

    private static String truncate(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
