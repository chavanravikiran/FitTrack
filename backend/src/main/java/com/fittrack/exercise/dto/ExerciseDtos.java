package com.fittrack.exercise.dto;

import com.fittrack.assessment.entity.AssessmentEnums.BodyArea;
import com.fittrack.exercise.entity.Exercise.Difficulty;
import com.fittrack.exercise.entity.Exercise.Location;

import java.util.List;

public final class ExerciseDtos {

    private ExerciseDtos() {
    }

    public record MuscleGroupDto(String code, String name) {
    }

    public record ExerciseSummary(Long id, String name, String slug, MuscleGroupDto muscleGroup, Difficulty difficulty,
                                  Location location, List<String> equipment, Integer defaultSets, Integer defaultReps,
                                  Integer defaultDurationSec, Integer defaultRestSec, boolean durationBased) {
    }

    public record ExerciseDetail(ExerciseSummary summary, String description, String instructions,
                                 String commonMistakes, String safetyPrecautions, List<MuscleGroupDto> secondaryMuscles,
                                 List<BodyArea> avoidIf, List<ExerciseSummary> alternatives, Long videoId) {
    }
}
