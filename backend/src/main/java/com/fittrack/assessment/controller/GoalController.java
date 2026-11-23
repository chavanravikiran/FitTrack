package com.fittrack.assessment.controller;

import com.fittrack.assessment.dto.GoalDtos.GoalRequest;
import com.fittrack.assessment.dto.GoalDtos.GoalResponse;
import com.fittrack.assessment.dto.GoalDtos.GoalStatusRequest;
import com.fittrack.assessment.dto.GoalDtos.GoalUpdateRequest;
import com.fittrack.assessment.entity.FitnessGoal.GoalStatus;
import com.fittrack.assessment.service.GoalService;
import com.fittrack.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Fitness goals")
@RestController
@RequestMapping("/api/v1/me/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalService;

    @Operation(summary = "List my goals, optionally filtered by status")
    @GetMapping
    public ApiResponse<List<GoalResponse>> list(@RequestParam(required = false) GoalStatus status) {
        return ApiResponse.ok(goalService.list(status));
    }

    @GetMapping("/{id}")
    public ApiResponse<GoalResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(goalService.get(id));
    }

    @Operation(summary = "Create a goal (replaces any active goal of the same type)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<GoalResponse> create(@Valid @RequestBody GoalRequest request) {
        return ApiResponse.ok(goalService.create(request), "Goal created");
    }

    @Operation(summary = "Edit an active goal")
    @PutMapping("/{id}")
    public ApiResponse<GoalResponse> update(@PathVariable Long id, @Valid @RequestBody GoalUpdateRequest request) {
        return ApiResponse.ok(goalService.update(id, request), "Goal updated");
    }

    @Operation(summary = "Mark a goal achieved, abandoned or active")
    @PatchMapping("/{id}/status")
    public ApiResponse<GoalResponse> status(@PathVariable Long id, @Valid @RequestBody GoalStatusRequest request) {
        return ApiResponse.ok(goalService.changeStatus(id, request.status()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        goalService.delete(id);
        return ApiResponse.message("Goal deleted");
    }
}
