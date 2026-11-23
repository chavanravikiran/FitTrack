package com.fittrack.user.service;

import com.fittrack.common.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgePolicyTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 1);

    @Test
    void acceptsAdults() {
        assertThatCode(() -> AgePolicy.validate(LocalDate.of(1990, 1, 1), TODAY)).doesNotThrowAnyException();
        assertThatCode(() -> AgePolicy.validate(LocalDate.of(2010, 6, 1), TODAY)).doesNotThrowAnyException();
    }

    @Test
    void rejectsUnder16() {
        assertThatThrownBy(() -> AgePolicy.validate(LocalDate.of(2010, 6, 2), TODAY))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void rejectsImplausibleAge() {
        assertThatThrownBy(() -> AgePolicy.validate(LocalDate.of(1900, 1, 1), TODAY))
                .isInstanceOf(BusinessRuleException.class);
    }
}
