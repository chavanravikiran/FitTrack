package com.fittrack.billing.entity;

import com.fittrack.billing.entity.BillingEnums.BillingCycle;
import com.fittrack.billing.entity.BillingEnums.Feature;
import com.fittrack.common.entity.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "subscription_plans")
public class SubscriptionPlan extends BaseEntity {

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "price_monthly_minor", nullable = false)
    private long priceMonthlyMinor;

    @Column(name = "price_yearly_minor", nullable = false)
    private long priceYearlyMinor;

    @Column(nullable = false, length = 3)
    private String currency = "INR";

    @Column(name = "is_free", nullable = false)
    private boolean free;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "plan_features", joinColumns = @JoinColumn(name = "plan_id"))
    private Set<PlanFeature> features = new HashSet<>();

    public long priceFor(BillingCycle cycle) {
        return cycle == BillingCycle.YEARLY ? priceYearlyMinor : priceMonthlyMinor;
    }

    public Optional<PlanFeature> feature(Feature feature) {
        return features.stream().filter(f -> f.getFeatureCode() == feature).findFirst();
    }
}
