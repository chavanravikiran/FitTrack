package com.fittrack.billing.entity;

import com.fittrack.billing.entity.BillingEnums.BillingCycle;
import com.fittrack.billing.entity.BillingEnums.SubscriptionSource;
import com.fittrack.billing.entity.BillingEnums.SubscriptionStatus;
import com.fittrack.common.entity.BaseEntity;
import com.fittrack.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * A user's plan for a period. Exactly one subscription per user is live (ACTIVE/TRIALING) at a time;
 * ended subscriptions are kept as history. The free plan has no end date.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "subscriptions")
public class Subscription extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private SubscriptionPlan plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SubscriptionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private SubscriptionSource source;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", length = 10)
    private BillingCycle billingCycle;

    @Column(name = "current_period_start", nullable = false)
    private Instant currentPeriodStart;

    @Column(name = "current_period_end")
    private Instant currentPeriodEnd;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(length = 255)
    private String notes;
}
