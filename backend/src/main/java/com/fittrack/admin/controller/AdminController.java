package com.fittrack.admin.controller;

import com.fittrack.admin.dto.AdminDtos.AdminStats;
import com.fittrack.admin.dto.AdminDtos.AdminUserDetail;
import com.fittrack.admin.dto.AdminDtos.AdminUserRow;
import com.fittrack.admin.dto.AdminDtos.GrantRequest;
import com.fittrack.admin.dto.AdminDtos.PlanUpdateRequest;
import com.fittrack.admin.dto.AdminDtos.UserStatusRequest;
import com.fittrack.admin.service.AdminService;
import com.fittrack.billing.dto.BillingDtos.PaymentDto;
import com.fittrack.billing.dto.BillingDtos.PlanDto;
import com.fittrack.billing.entity.BillingEnums.PaymentStatus;
import com.fittrack.common.api.ApiResponse;
import com.fittrack.common.api.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Admin")
@Validated
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @Operation(summary = "Key SaaS metrics: users, premium, trials, revenue, MRR, signups")
    @GetMapping("/stats")
    public ApiResponse<AdminStats> stats() {
        return ApiResponse.ok(adminService.stats());
    }

    @Operation(summary = "Search users (email or name), filter by plan and enabled status")
    @GetMapping("/users")
    public ApiResponse<PageResponse<AdminUserRow>> users(
            @RequestParam(required = false) @Size(max = 100) String q,
            @RequestParam(required = false) @Size(max = 20) String plan,
            @RequestParam(defaultValue = "ANY") String status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.ok(adminService.users(q, plan, status, page, size));
    }

    @GetMapping("/users/{id}")
    public ApiResponse<AdminUserDetail> user(@PathVariable Long id) {
        return ApiResponse.ok(adminService.userDetail(id));
    }

    @Operation(summary = "Enable or disable a user (disabling signs them out everywhere)")
    @PatchMapping("/users/{id}/status")
    public ApiResponse<AdminUserRow> status(@PathVariable Long id, @Valid @RequestBody UserStatusRequest request) {
        return ApiResponse.ok(adminService.setEnabled(id, request.enabled()), "User updated");
    }

    @Operation(summary = "Grant a paid plan for N days (e.g. support, promotions)")
    @PostMapping("/users/{id}/grant")
    public ApiResponse<AdminUserRow> grant(@PathVariable Long id, @Valid @RequestBody GrantRequest request) {
        return ApiResponse.ok(adminService.grant(id, request), "Plan granted");
    }

    @Operation(summary = "Move a user to the free plan immediately")
    @PostMapping("/users/{id}/revoke")
    public ApiResponse<AdminUserRow> revoke(@PathVariable Long id) {
        return ApiResponse.ok(adminService.revoke(id), "User moved to the free plan");
    }

    @GetMapping("/plans")
    public ApiResponse<List<PlanDto>> plans() {
        return ApiResponse.ok(adminService.plans());
    }

    @Operation(summary = "Edit a plan's name, prices (minor units, e.g. paise), status and features")
    @PutMapping("/plans/{code}")
    public ApiResponse<PlanDto> updatePlan(@PathVariable String code, @Valid @RequestBody PlanUpdateRequest request) {
        return ApiResponse.ok(adminService.updatePlan(code, request), "Plan saved");
    }

    @GetMapping("/payments")
    public ApiResponse<PageResponse<PaymentDto>> payments(
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.ok(adminService.payments(status, page, size));
    }
}
