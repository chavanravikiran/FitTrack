package com.fittrack.recommendation.calculator;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class BodyMetricsTest {

    @Test
    void computesBmi() {
        // 70 kg / (1.75 m)^2 = 22.86
        assertThat(BodyMetrics.bmi(new BigDecimal("70"), new BigDecimal("175"))).isEqualByComparingTo("22.9");
    }

    @Test
    void minHealthyWeightKeepsBmiAtLeast18_5() {
        BigDecimal min = BodyMetrics.minHealthyWeight(new BigDecimal("170"));
        assertThat(min).isEqualByComparingTo("53.5");
        assertThat(BodyMetrics.bmi(min, new BigDecimal("170"))).isGreaterThanOrEqualTo(new BigDecimal("18.5"));
    }
}
