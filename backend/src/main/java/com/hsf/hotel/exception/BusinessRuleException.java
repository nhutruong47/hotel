package com.hsf.hotel.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a domain-level business rule is violated. Mapped to HTTP 422.
 */
public class BusinessRuleException extends ApiException {
    public BusinessRuleException(String code, String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, code, message);
    }
}
