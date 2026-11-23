package com.fittrack.billing.dto;

import com.fittrack.billing.entity.BillingEnums.BillingCycle;
import com.fittrack.billing.entity.BillingEnums.Feature;
import com.fittrack.billing.entity.BillingEnums.PaymentStatus;
import com.fittrack.billing.entity.BillingEnums.SubscriptionSource;
import com.fittrack.billing.entity.BillingEnums.SubscriptionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class BillingDtos {

    private BillingDtos() {
    }

    public record FeatureDto(Feature code, Integer limit) {
    }

    public record PlanDto(String code, String name, String description, long priceMonthlyMinor,
                          long priceYearlyMinor, String currency, boolean free, boolean active,
                          List<FeatureDto> features) {
    }

    public record SubscriptionDto(String planCode, String planName, boolean premium, SubscriptionStatus status,
                                  SubscriptionSource source, BillingCycle billingCycle,
                                  Instant currentPeriodStart, Instant currentPeriodEnd, Long daysRemaining,
                                  List<FeatureDto> features) {
    }

    public record BillingConfigDto(boolean paymentsEnabled, int trialDays, String provider) {
    }

    public record CheckoutRequest(
            @NotBlank @Size(max = 20) @Pattern(regexp = "[A-Z_]+") String planCode,
            @NotNull BillingCycle billingCycle) {
    }

    /** Everything the browser needs to open Razorpay Checkout. Amount is decided server-side. */
    public record CheckoutResponse(String provider, String keyId, String orderId, long amountMinor, String currency,
                                   String productName, String description, String prefillName, String prefillEmail) {
    }

    public record VerifyPaymentRequest(
            @NotBlank @Size(max = 64) String orderId,
            @NotBlank @Size(max = 64) String paymentId,
            @NotBlank @Size(max = 128) String signature) {
    }

    public record PaymentDto(Long id, String planCode, String planName, BillingCycle billingCycle, long amountMinor,
                             String currency, PaymentStatus status, String providerOrderId, String providerPaymentId,
                             Instant paidAt, Instant createdAt) {
    }
}
