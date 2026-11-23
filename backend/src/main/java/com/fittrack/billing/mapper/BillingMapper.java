package com.fittrack.billing.mapper;

import com.fittrack.billing.dto.BillingDtos.FeatureDto;
import com.fittrack.billing.dto.BillingDtos.PaymentDto;
import com.fittrack.billing.dto.BillingDtos.PlanDto;
import com.fittrack.billing.dto.BillingDtos.SubscriptionDto;
import com.fittrack.billing.entity.Payment;
import com.fittrack.billing.entity.PlanFeature;
import com.fittrack.billing.entity.Subscription;
import com.fittrack.billing.entity.SubscriptionPlan;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Component
public class BillingMapper {

    public PlanDto toDto(SubscriptionPlan p) {
        return new PlanDto(p.getCode(), p.getName(), p.getDescription(), p.getPriceMonthlyMinor(),
                p.getPriceYearlyMinor(), p.getCurrency(), p.isFree(), p.isActive(), features(p.getFeatures()));
    }

    public SubscriptionDto toDto(Subscription s, Instant now) {
        Long daysRemaining = s.getCurrentPeriodEnd() == null ? null
                : Math.max(0, (long) Math.ceil(Duration.between(now, s.getCurrentPeriodEnd()).toHours() / 24.0));
        SubscriptionPlan p = s.getPlan();
        return new SubscriptionDto(p.getCode(), p.getName(), !p.isFree(), s.getStatus(), s.getSource(),
                s.getBillingCycle(), s.getCurrentPeriodStart(), s.getCurrentPeriodEnd(), daysRemaining,
                features(p.getFeatures()));
    }

    public PaymentDto toDto(Payment p) {
        return new PaymentDto(p.getId(), p.getPlan().getCode(), p.getPlan().getName(), p.getBillingCycle(),
                p.getAmountMinor(), p.getCurrency(), p.getStatus(), p.getProviderOrderId(),
                p.getProviderPaymentId(), p.getPaidAt(), p.getCreatedAt());
    }

    private List<FeatureDto> features(Set<PlanFeature> features) {
        return features.stream()
                .sorted(Comparator.comparing(PlanFeature::getFeatureCode))
                .map(f -> new FeatureDto(f.getFeatureCode(), f.getLimitValue()))
                .toList();
    }
}
