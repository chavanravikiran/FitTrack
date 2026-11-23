package com.fittrack.exercise.controller;

import com.fittrack.common.api.ApiResponse;
import com.fittrack.exercise.dto.ExerciseDtos.ExerciseDetail;
import com.fittrack.exercise.dto.ExerciseDtos.ExerciseSummary;
import com.fittrack.exercise.dto.ExerciseDtos.MuscleGroupDto;
import com.fittrack.exercise.entity.Exercise.Difficulty;
import com.fittrack.exercise.service.ExerciseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Exercises")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ExerciseController {

    private final ExerciseService exerciseService;

    @GetMapping("/muscle-groups")
    public ApiResponse<List<MuscleGroupDto>> muscleGroups() {
        return ApiResponse.ok(exerciseService.muscleGroups());
    }

    @Operation(summary = "Search the exercise catalog")
    @GetMapping("/exercises")
    public ApiResponse<List<ExerciseSummary>> search(@RequestParam(required = false) String muscle,
                                                     @RequestParam(required = false) Difficulty difficulty,
                                                     @RequestParam(required = false) String equipment,
                                                     @RequestParam(required = false) String q) {
        return ApiResponse.ok(exerciseService.search(muscle, difficulty, equipment, q));
    }

    @Operation(summary = "Exercise details: instructions, common mistakes, safety, alternatives")
    @GetMapping("/exercises/{id}")
    public ApiResponse<ExerciseDetail> detail(@PathVariable Long id) {
        return ApiResponse.ok(exerciseService.detail(id));
    }

    @GetMapping("/exercises/{id}/alternatives")
    public ApiResponse<List<ExerciseSummary>> alternatives(@PathVariable Long id) {
        return ApiResponse.ok(exerciseService.alternatives(id));
    }
}
