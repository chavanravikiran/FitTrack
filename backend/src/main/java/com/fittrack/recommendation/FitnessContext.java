package com.fittrack.recommendation;

import com.fittrack.assessment.entity.FitnessAssessment;
import com.fittrack.user.entity.UserProfile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;

/** Everything personalisation logic needs about a user, loaded once per request. */
public record FitnessContext(Long userId, UserProfile profile, FitnessAssessment assessment, BigDecimal weightKg,
                             Integer age, LocalDate today, ZoneId zone) {

    public boolean hasAssessment() {
        return assessment != null;
    }

    public double weightOr(double fallback) {
        return weightKg == null ? fallback : weightKg.doubleValue();
    }
}
