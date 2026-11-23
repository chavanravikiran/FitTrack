package com.fittrack.workout.controller;

import com.fittrack.common.api.ApiResponse;
import com.fittrack.common.api.PageResponse;
import com.fittrack.workout.dto.WorkoutDtos.CalendarEntryDto;
import com.fittrack.workout.dto.WorkoutDtos.CompleteRequest;
import com.fittrack.workout.dto.WorkoutDtos.DateRequest;
import com.fittrack.workout.dto.WorkoutDtos.DayWorkoutDto;
import com.fittrack.workout.dto.WorkoutDtos.HistoryItemDto;
import com.fittrack.workout.dto.WorkoutDtos.LogSetsRequest;
import com.fittrack.workout.dto.WorkoutDtos.PersonalRecordDto;
import com.fittrack.workout.dto.WorkoutDtos.PlanDto;
import com.fittrack.workout.dto.WorkoutDtos.PlanExerciseDto;
import com.fittrack.workout.dto.WorkoutDtos.RescheduleRequest;
import com.fittrack.workout.dto.WorkoutDtos.SkipRequest;
import com.fittrack.workout.dto.WorkoutDtos.SummaryDto;
import com.fittrack.workout.dto.WorkoutDtos.SwapExerciseRequest;
import com.fittrack.workout.service.WorkoutPlanService;
import com.fittrack.workout.service.WorkoutSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Workouts")
@Validated
@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class WorkoutController {

    private final WorkoutPlanService planService;
    private final WorkoutSessionService sessionService;

    @Operation(summary = "My active workout plan (generated from the assessment on first use)")
    @GetMapping("/workout-plan")
    public ApiResponse<PlanDto> plan() {
        return ApiResponse.ok(planService.activePlan());
    }

    @Operation(summary = "Rebuild my plan from my current assessment")
    @PostMapping("/workout-plan/regenerate")
    public ApiResponse<PlanDto> regenerate() {
        return ApiResponse.ok(planService.regenerate(), "Your plan has been rebuilt");
    }

    @Operation(summary = "Premium: replace an exercise in my plan")
    @PutMapping("/workout-plan/exercises/{planExerciseId}")
    public ApiResponse<PlanExerciseDto> swap(@PathVariable Long planExerciseId, @Valid @RequestBody SwapExerciseRequest req) {
        return ApiResponse.ok(planService.swapExercise(planExerciseId, req.exerciseId()), "Exercise updated");
    }

    @Operation(summary = "Workout for a date (defaults to today in my time zone)")
    @GetMapping("/workouts/day")
    public ApiResponse<DayWorkoutDto> day(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                          @RequestParam(required = false) Long planDayId) {
        return ApiResponse.ok(sessionService.day(date, planDayId));
    }

    @Operation(summary = "Calendar of planned, completed, skipped and moved workouts")
    @GetMapping("/workouts/calendar")
    public ApiResponse<List<CalendarEntryDto>> calendar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(sessionService.calendar(from, to));
    }

    @GetMapping("/workouts/summary")
    public ApiResponse<SummaryDto> summary() {
        return ApiResponse.ok(sessionService.summary());
    }

    @PostMapping("/workouts/start")
    public ApiResponse<DayWorkoutDto> start(@Valid @RequestBody DateRequest req) {
        return ApiResponse.ok(sessionService.start(req.date(), req.planDayId()), "Workout started - you've got this!");
    }

    @Operation(summary = "Log or update sets for a session")
    @PutMapping("/workouts/sessions/{sessionId}/sets")
    public ApiResponse<DayWorkoutDto> logSets(@PathVariable Long sessionId, @Valid @RequestBody LogSetsRequest req) {
        return ApiResponse.ok(sessionService.logSets(sessionId, req.sets()));
    }

    @PostMapping("/workouts/sessions/{sessionId}/complete")
    public ApiResponse<DayWorkoutDto> complete(@PathVariable Long sessionId, @Valid @RequestBody(required = false) CompleteRequest req) {
        return ApiResponse.ok(sessionService.complete(sessionId, req), "Great job - workout complete!");
    }

    @PostMapping("/workouts/skip")
    public ApiResponse<CalendarEntryDto> skip(@Valid @RequestBody SkipRequest req) {
        return ApiResponse.ok(sessionService.skip(req.date(), req.planDayId(), req.reason()), "Workout skipped");
    }

    @PostMapping("/workouts/reschedule")
    public ApiResponse<CalendarEntryDto> reschedule(@Valid @RequestBody RescheduleRequest req) {
        return ApiResponse.ok(sessionService.reschedule(req.date(), req.planDayId(), req.newDate()), "Workout moved");
    }

    @GetMapping("/workouts/history")
    public ApiResponse<PageResponse<HistoryItemDto>> history(@RequestParam(defaultValue = "0") @Min(0) int page,
                                                             @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.ok(sessionService.history(page, size));
    }

    @GetMapping("/personal-records")
    public ApiResponse<List<PersonalRecordDto>> personalRecords() {
        return ApiResponse.ok(sessionService.personalRecords());
    }
}
