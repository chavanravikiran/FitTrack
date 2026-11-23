package com.fittrack.common.exception;

import org.springframework.http.HttpStatus;

/** A syntactically valid request that violates a domain or safety rule. */
public class BusinessRuleException extends ApiException {

    public BusinessRuleException(String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, message);
    }
}
