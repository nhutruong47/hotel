package com.hsf.hotel.exception;

import org.springframework.http.HttpStatus;

/**
 * Base class for all domain-level exceptions. Carries an HTTP status and
 * a stable machine-readable code so the global exception handler can map
 * it to a uniform API response without leaking internals.
 *
 * <p>All custom exception types in this package extend this class. The
 * legacy {@code BusinessException} is preserved as a deprecated alias for
 * source compatibility with services that pre-date the consolidation.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public ApiException(HttpStatus status, String code, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}