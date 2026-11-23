package com.fittrack.billing.service;

import com.fittrack.billing.entity.BillingEnums.Feature;
import com.fittrack.billing.entity.PlanFeature;
import com.fittrack.billing.entity.SubscriptionPlan;
import com.fittrack.common.exception.PlanLimitException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Single place that answers "is this user allowed to do X?" based on their live plan.
 * Feature services call {@link #requireFeature} / {@link #checkLimit} before doing gated work.
 */
@Service
@RequiredArgsConstructor
public class EntitlementService {

    private final SubscriptionService subscriptionService;

    @Transactional
    public SubscriptionPlan planOf(Long userId) {
        return subscriptionService.current(userId).getPlan();
    }

    @Transactional
    public boolean has(Long userId, Feature feature) {
        return planOf(userId).feature(feature).isPresent();
    }

    /** Empty optional = feature not included; present with null = unlimited. */
    @Transactional
    public Optional<PlanFeature> feature(Long userId, Feature feature) {
        return planOf(userId).feature(feature);
    }

    @Transactional
    public void requireFeature(Long userId, Feature feature, String featureLabel) {
        if (!has(userId, feature)) {
            throw new PlanLimitException(featureLabel + " is a Premium feature. Upgrade your plan to unlock it.");
        }
    }

    /** Throws if adding one more item would exceed the plan limit for {@code feature}. */
    @Transactional
    public void checkLimit(Long userId, Feature feature, long currentCount, String itemLabel) {
        PlanFeature pf = feature(userId, feature)
                .orElseThrow(() -> new PlanLimitException("Your plan doesn't include " + itemLabel + ". Upgrade to unlock it."));
        Integer limit = pf.getLimitValue();
        if (limit != null && currentCount >= limit) {
            throw new PlanLimitException("Your plan allows up to %d %s. Upgrade to Premium for unlimited %s."
                    .formatted(limit, itemLabel, itemLabel));
        }
    }
}
