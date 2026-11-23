package com.fittrack.assessment.controller;

import com.fittrack.assessment.dto.AssessmentDtos.EquipmentDto;
import com.fittrack.assessment.service.AssessmentService;
import com.fittrack.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Reference data")
@RestController
@RequestMapping("/api/v1/equipment")
@RequiredArgsConstructor
public class EquipmentController {

    private final AssessmentService assessmentService;

    @Operation(summary = "List equipment options")
    @GetMapping
    public ApiResponse<List<EquipmentDto>> list() {
        return ApiResponse.ok(assessmentService.listEquipment());
    }
}
