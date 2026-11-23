package com.fittrack.assessment.controller;

import com.fittrack.assessment.dto.AssessmentDtos.AssessmentRequest;
import com.fittrack.assessment.dto.AssessmentDtos.AssessmentResponse;
import com.fittrack.assessment.dto.AssessmentDtos.AssessmentSummary;
import com.fittrack.assessment.dto.AssessmentDtos.OnboardingRequest;
import com.fittrack.assessment.service.AssessmentService;
import com.fittrack.assessment.service.OnboardingService;
import com.fittrack.common.api.ApiResponse;
import com.fittrack.common.api.PageResponse;
import com.fittrack.user.dto.ProfileDtos.ProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Fitness assessment")
@Validated
@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class AssessmentController {

    private final AssessmentService assessmentService;
    private final OnboardingService onboardingService;

    @Operation(summary = "Complete onboarding: profile, first assessment, starting weight and target weight")
    @PostMapping("/onboarding")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProfileResponse> onboarding(@Valid @RequestBody OnboardingRequest request) {
        return ApiResponse.ok(onboardingService.complete(request), "Welcome to FitTrack!");
    }

    @Operation(summary = "Get my current fitness assessment")
    @GetMapping("/assessment")
    public ApiResponse<AssessmentResponse> current() {
        return ApiResponse.ok(assessmentService.getCurrent());
    }

    @Operation(summary = "Update my fitness assessment (stores a new version)")
    @PostMapping("/assessment")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AssessmentResponse> reassess(@Valid @RequestBody AssessmentRequest request) {
        return ApiResponse.ok(assessmentService.reassess(request), "Assessment updated");
    }

    @Operation(summary = "List previous assessments, newest first")
    @GetMapping("/assessment/history")
    public ApiResponse<PageResponse<AssessmentSummary>> history(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        return ApiResponse.ok(assessmentService.history(page, size));
    }

    @Operation(summary = "Get one of my assessments by id")
    @GetMapping("/assessment/{id}")
    public ApiResponse<AssessmentResponse> byId(@PathVariable Long id) {
        return ApiResponse.ok(assessmentService.getById(id));
    }
}
