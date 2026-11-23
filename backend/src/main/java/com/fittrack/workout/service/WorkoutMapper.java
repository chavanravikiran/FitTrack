package com.fittrack.workout.service;

import com.fittrack.exercise.service.ExerciseService;
import com.fittrack.workout.dto.WorkoutDtos.PlanDayDto;
import com.fittrack.workout.dto.WorkoutDtos.PlanDto;
import com.fittrack.workout.dto.WorkoutDtos.PlanExerciseDto;
import com.fittrack.workout.entity.WorkoutExercise;
import com.fittrack.workout.entity.WorkoutPlan;
import com.fittrack.workout.entity.WorkoutPlanDay;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

final class WorkoutMapper {

    private WorkoutMapper() {
    }

    static PlanDto toDto(WorkoutPlan p) {
        List<String> explanations = p.getNotes() == null ? List.of()
                : Arrays.stream(p.getNotes().split("\n")).filter(s -> !s.isBlank()).toList();
        return new PlanDto(p.getId(), p.getName(), p.getSplitType(), p.getStartDate(), explanations,
                p.getDays().stream().sorted(Comparator.comparingInt(WorkoutPlanDay::getDayOfWeek)).map(WorkoutMapper::toDto).toList());
    }

    static PlanDayDto toDto(WorkoutPlanDay d) {
        return new PlanDayDto(d.getId(), d.getDayOfWeek(), d.getTitle(), d.getFocus(), d.isRestDay(),
                d.getExercises().stream().sorted(Comparator.comparingInt(WorkoutExercise::getSortOrder))
                        .map(WorkoutMapper::toDto).toList());
    }

    static PlanExerciseDto toDto(WorkoutExercise e) {
        return new PlanExerciseDto(e.getId(), ExerciseService.toSummary(e.getExercise()), e.getSets(), e.getRepsMin(),
                e.getRepsMax(), e.getDurationSec(), e.getRestSec(), e.getTargetRpe(), e.getNotes());
    }

    /** Rough session length in minutes: work + rest + 10 minutes warm-up/cool-down. */
    static int estimateMinutes(List<WorkoutExercise> exercises) {
        int seconds = 600;
        for (WorkoutExercise e : exercises) {
            int work = e.getDurationSec() != null ? e.getDurationSec()
                    : (e.getRepsMax() == null ? 10 : e.getRepsMax()) * 4;
            seconds += e.getSets() * (work + e.getRestSec());
        }
        return Math.round(seconds / 60f);
    }
}
