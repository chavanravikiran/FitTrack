package com.fittrack.recommendation.calculator;

import com.fittrack.assessment.entity.AssessmentEnums.ActivityLevel;
import com.fittrack.user.entity.Gender;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BodyMetricsBmrTest {

    @Test
    void mifflinStJeor() {
        // 10*70 + 6.25*175 - 5*30 = 1643.75
        assertThat(BodyMetrics.bmr(70, 175, 30, Gender.MALE)).isEqualTo(1649);
        assertThat(BodyMetrics.bmr(70, 175, 30, Gender.FEMALE)).isEqualTo(1483);
        // prefer-not-to-say uses the midpoint constant (-78)
        assertThat(BodyMetrics.bmr(70, 175, 30, Gender.PREFER_NOT_TO_SAY)).isEqualTo(1566);
    }

    @Test
    void tdeeAppliesActivityMultiplier() {
        assertThat(BodyMetrics.tdee(1500, ActivityLevel.SEDENTARY)).isEqualTo(1800);
        assertThat(BodyMetrics.tdee(1500, ActivityLevel.MODERATELY_ACTIVE)).isEqualTo(2325);
    }
}
