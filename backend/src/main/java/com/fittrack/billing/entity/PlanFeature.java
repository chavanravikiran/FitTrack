package com.fittrack.billing.entity;

import com.fittrack.billing.entity.BillingEnums.Feature;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "featureCode")
@Embeddable
public class PlanFeature {

    @Enumerated(EnumType.STRING)
    @Column(name = "feature_code", nullable = false, length = 40)
    private Feature featureCode;

    /** NULL = unlimited. */
    @Column(name = "limit_value")
    private Integer limitValue;
}
