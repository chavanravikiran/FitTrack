package com.fittrack.billing.entity;

public final class BillingEnums {

    private BillingEnums() {
    }

    /** Capabilities a plan can include. A NULL limit on a plan feature means unlimited. */
    public enum Feature {
        ACTIVE_GOALS,
        ADVANCED_ANALYTICS,
        PDF_REPORTS,
        PROGRESS_PHOTOS,
        CUSTOM_WORKOUTS,
        DATA_EXPORT,
        AI_ASSISTANT
    }

    public enum SubscriptionStatus {
        ACTIVE, TRIALING, EXPIRED, CANCELED;

        public boolean isLive() {
            return this == ACTIVE || this == TRIALING;
        }
    }

    public enum SubscriptionSource { FREE, TRIAL, PAYMENT, ADMIN_GRANT }

    public enum BillingCycle {
        MONTHLY, YEARLY
    }

    public enum PaymentStatus { CREATED, PAID, FAILED }

    public enum PaymentProvider { RAZORPAY }
}
