package com.fittrack.user.service;

import com.fittrack.common.exception.BusinessRuleException;

import java.time.LocalDate;
import java.time.Period;

/** FitTrack's guidance is designed for adults; younger users should train under supervision. */
public final class AgePolicy {

    public static final int MIN_AGE = 16;
    public static final int MAX_AGE = 100;

    private AgePolicy() {
    }

    public static void validate(LocalDate dateOfBirth, LocalDate today) {
        int age = Period.between(dateOfBirth, today).getYears();
        if (age < MIN_AGE) {
            throw new BusinessRuleException("FitTrack is designed for users aged " + MIN_AGE
                    + " and over. Please train under the guidance of a qualified coach or parent.");
        }
        if (age > MAX_AGE) {
            throw new BusinessRuleException("Please check your date of birth");
        }
    }
}
