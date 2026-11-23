package com.fittrack.billing.service;

import com.fittrack.billing.entity.BillingEnums.BillingCycle;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class SubscriptionServiceTest {

    @Test
    void monthlyCycleIsCalendarAware() {
        assertThat(SubscriptionService.addCycle(Instant.parse("2026-01-31T10:00:00Z"), BillingCycle.MONTHLY))
                .isEqualTo(Instant.parse("2026-02-28T10:00:00Z"));
        assertThat(SubscriptionService.addCycle(Instant.parse("2026-03-15T00:00:00Z"), BillingCycle.MONTHLY))
                .isEqualTo(Instant.parse("2026-04-15T00:00:00Z"));
    }

    @Test
    void yearlyCycleHandlesLeapDay() {
        assertThat(SubscriptionService.addCycle(Instant.parse("2028-02-29T08:00:00Z"), BillingCycle.YEARLY))
                .isEqualTo(Instant.parse("2029-02-28T08:00:00Z"));
    }
}
