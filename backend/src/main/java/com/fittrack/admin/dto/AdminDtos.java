package com.fittrack.admin.dto;

import com.fittrack.billing.dto.BillingDtos.FeatureDto;
import com.fittrack.billing.dto.BillingDtos.PaymentDto;
import com.fittrack.billing.entity.BillingEnums.BillingCycle;
import com.fittrack.billing.entity.BillingEnums.SubscriptionSource;
import com.fittrack.billing.entity.BillingEnums.SubscriptionStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class AdminDtos {

    private AdminDtos() {
    }

    public record DayCount(LocalDate date, long count) {
    }

    public record AdminStats(long totalUsers, long newUsersLast30Days, long activeUsersLast30Days,
                             long premiumUsers, long trialingUsers, long revenueLast30DaysMinor, long mrrMinor,
                             String currency, boolean paymentsEnabled, List<DayCount> signupsLast30Days) {
    }

    /** Row in the admin user table (built by a JPQL constructor expression). */
    public record AdminUserRow(Long id, String email, String fullName, boolean enabled, boolean emailVerified,
                               boolean onboardingCompleted, String planCode, SubscriptionStatus subscriptionStatus,
                               Instant periodEnd, Instant createdAt, Instant lastLoginAt) {
    }

    public record SubscriptionHistoryDto(Long id, String planCode, SubscriptionStatus status, SubscriptionSource source,
                                         BillingCycle billingCycle, Instant currentPeriodStart, Instant currentPeriodEnd,
                                         Instant endedAt, String notes) {
    }

    public record AdminUserDetail(AdminUserRow user, List<String> roles, String termsVersion, Instant termsAcceptedAt,
                                  List<SubscriptionHistoryDto> subscriptions, List<PaymentDto> payments) {
    }

    public record UserStatusRequest(@NotNull Boolean enabled) {
    }

    public record GrantRequest(
            @NotBlank @Size(max = 20) String planCode,
            @NotNull @Min(1) @Max(3650) Integer days,
            @Size(max = 255) String note) {
    }

    public record PlanUpdateRequest(
            @NotBlank @Size(max = 60) String name,
            @Size(max = 500) String description,
            @NotNull @PositiveOrZero @Max(100_000_000) Long priceMonthlyMinor,
            @NotNull @PositiveOrZero @Max(1_000_000_000) Long priceYearlyMinor,
            @NotNull Boolean active,
            @NotNull @Size(max = 20) List<@Valid FeatureDto> features) {
    }
}
