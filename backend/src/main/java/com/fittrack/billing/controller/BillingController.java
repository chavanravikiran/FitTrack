package com.fittrack.billing.controller;

import com.fittrack.billing.dto.BillingDtos.BillingConfigDto;
import com.fittrack.billing.dto.BillingDtos.CheckoutRequest;
import com.fittrack.billing.dto.BillingDtos.CheckoutResponse;
import com.fittrack.billing.dto.BillingDtos.PaymentDto;
import com.fittrack.billing.dto.BillingDtos.PlanDto;
import com.fittrack.billing.dto.BillingDtos.SubscriptionDto;
import com.fittrack.billing.dto.BillingDtos.VerifyPaymentRequest;
import com.fittrack.billing.service.BillingService;
import com.fittrack.billing.service.CheckoutService;
import com.fittrack.common.api.ApiResponse;
import com.fittrack.common.api.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Billing")
@Validated
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;
    private final CheckoutService checkoutService;

    @Operation(summary = "Public: available plans and prices")
    @GetMapping("/plans")
    public ApiResponse<List<PlanDto>> plans() {
        return ApiResponse.ok(billingService.publicPlans());
    }

    @Operation(summary = "Public: whether online payments are enabled and the trial length")
    @GetMapping("/billing/config")
    public ApiResponse<BillingConfigDto> config() {
        return ApiResponse.ok(billingService.config());
    }

    @Operation(summary = "My current plan, status and features")
    @GetMapping("/me/subscription")
    public ApiResponse<SubscriptionDto> subscription() {
        return ApiResponse.ok(billingService.mySubscription());
    }

    @Operation(summary = "Start a checkout: creates a Razorpay order priced by the server")
    @PostMapping("/me/billing/checkout")
    public ApiResponse<CheckoutResponse> checkout(@Valid @RequestBody CheckoutRequest request) {
        return ApiResponse.ok(checkoutService.createCheckout(request));
    }

    @Operation(summary = "Confirm a completed Razorpay payment (signature verified server-side)")
    @PostMapping("/me/billing/verify")
    public ApiResponse<SubscriptionDto> verify(@Valid @RequestBody VerifyPaymentRequest request) {
        return ApiResponse.ok(checkoutService.verify(request), "Payment successful - welcome to Premium!");
    }

    @Operation(summary = "My payment history")
    @GetMapping("/me/billing/payments")
    public ApiResponse<PageResponse<PaymentDto>> payments(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.ok(billingService.myPayments(page, size));
    }
}
