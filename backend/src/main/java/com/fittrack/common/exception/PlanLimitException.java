package com.fittrack.common.exception;

import org.springframework.http.HttpStatus;

/** The current plan does not include a feature or its limit is reached (HTTP 402). */
public class PlanLimitException extends ApiException {

    public PlanLimitException(String message) {
        super(HttpStatus.PAYMENT_REQUIRED, message);
    }
}
