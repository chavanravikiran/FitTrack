package com.fittrack.activity.controller;

import com.fittrack.activity.dto.ActivityDtos.ActivityDayDto;
import com.fittrack.activity.dto.ActivityDtos.ActivitySummaryDto;
import com.fittrack.activity.dto.ActivityDtos.HealthImportRequest;
import com.fittrack.activity.dto.ActivityDtos.RecoveryDto;
import com.fittrack.activity.dto.ActivityDtos.SleepDto;
import com.fittrack.activity.dto.ActivityDtos.SleepRequest;
import com.fittrack.activity.dto.ActivityDtos.StepEntryRequest;
import com.fittrack.activity.dto.ActivityDtos.WaterAddRequest;
import com.fittrack.activity.dto.ActivityDtos.WaterDayDto;
import com.fittrack.activity.dto.ActivityDtos.WaterGoalRequest;
import com.fittrack.activity.dto.ActivityDtos.WaterHistoryDayDto;
import com.fittrack.activity.service.SleepService;
import com.fittrack.activity.service.StepService;
import com.fittrack.activity.service.WaterService;
import com.fittrack.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
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

@Tag(name = "Activity, water & sleep")
@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class ActivityController {

    private final StepService stepService;
    private final WaterService waterService;
    private final SleepService sleepService;

    // ---- steps ----

    @Operation(summary = "Record steps for a date (manual entry; one record per day)")
    @PutMapping("/activity/{date}")
    public ApiResponse<ActivityDayDto> recordSteps(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                                   @Valid @RequestBody StepEntryRequest req) {
        return ApiResponse.ok(stepService.record(date, req), "Steps saved");
    }

    @Operation(summary = "Steps summary for the last N days (7 = weekly, 30 = monthly)")
    @GetMapping("/activity/summary")
    public ApiResponse<ActivitySummaryDto> summary(@RequestParam(defaultValue = "7") int days) {
        return ApiResponse.ok(stepService.summary(days));
    }

    @Operation(summary = "Import daily totals from a health app (for a future mobile companion app)")
    @PostMapping("/integrations/health/import")
    public ApiResponse<List<ActivityDayDto>> importHealth(@Valid @RequestBody HealthImportRequest req) {
        return ApiResponse.ok(stepService.importFromHealthApp(req));
    }

    // ---- water ----

    @Operation(summary = "Add water (250 ml, 500 ml or a custom amount)")
    @PostMapping("/water")
    public ApiResponse<WaterDayDto> addWater(@Valid @RequestBody WaterAddRequest req) {
        return ApiResponse.ok(waterService.add(req.amountMl(), req.date()));
    }

    @DeleteMapping("/water/{entryId}")
    public ApiResponse<WaterDayDto> deleteWater(@PathVariable Long entryId) {
        return ApiResponse.ok(waterService.delete(entryId));
    }

    @Operation(summary = "Water for a date: goal, total, remaining, entries and guidance (default today)")
    @GetMapping("/water")
    public ApiResponse<WaterDayDto> waterDay(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(waterService.day(date));
    }

    @Operation(summary = "Adjust the water goal for a date (e.g. hot weather)")
    @PutMapping("/water/goal/{date}")
    public ApiResponse<WaterDayDto> waterGoal(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                              @Valid @RequestBody WaterGoalRequest req) {
        return ApiResponse.ok(waterService.setGoal(date, req), "Goal updated");
    }

    @GetMapping("/water/history")
    public ApiResponse<List<WaterHistoryDayDto>> waterHistory(@RequestParam(defaultValue = "7") int days) {
        return ApiResponse.ok(waterService.history(days));
    }

    // ---- sleep ----

    @Operation(summary = "Log the night that ended on this date (bed and wake times in HH:mm, local time)")
    @PutMapping("/sleep/{date}")
    public ApiResponse<SleepDto> recordSleep(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                             @Valid @RequestBody SleepRequest req) {
        return ApiResponse.ok(sleepService.record(date, req), "Sleep saved");
    }

    @GetMapping("/sleep")
    public ApiResponse<List<SleepDto>> sleepHistory(@RequestParam(defaultValue = "14") int days) {
        return ApiResponse.ok(sleepService.history(days));
    }

    @DeleteMapping("/sleep/{date}")
    public ApiResponse<Void> deleteSleep(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        sleepService.delete(date);
        return ApiResponse.message("Sleep entry removed");
    }

    @Operation(summary = "Recovery status and suggested intensity based on sleep and recent workouts")
    @GetMapping("/recovery")
    public ApiResponse<RecoveryDto> recovery() {
        return ApiResponse.ok(sleepService.recovery());
    }
}
