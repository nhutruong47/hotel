package com.hsf.hotel.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a user attempts an action they are not allowed to perform
 * (resource ownership violation, role check, etc). Mapped to HTTP 403.
 */
public class ForbiddenException extends ApiException {
    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, "FORBIDDEN", message);
    }
}
