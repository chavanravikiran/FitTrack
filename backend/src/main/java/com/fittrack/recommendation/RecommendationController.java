package com.fittrack.recommendation;

import com.fittrack.common.api.ApiResponse;
import com.fittrack.recommendation.dto.RecommendationDtos.RecommendationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Recommendations")
@RestController
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @Operation(summary = "Personalised, explained recommendations: schedule, sets/reps, intensity, steps, water, nutrition, sleep")
    @GetMapping("/api/v1/me/recommendations")
    public ApiResponse<RecommendationResponse> recommendations() {
        return ApiResponse.ok(recommendationService.forCurrentUser());
    }
}
