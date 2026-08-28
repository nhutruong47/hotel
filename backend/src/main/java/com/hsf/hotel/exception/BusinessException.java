package com.hsf.hotel.exception;

import org.springframework.http.HttpStatus;

/**
 * Generic business-level exception. Use the more specific subclasses where
 * possible ({@link ResourceNotFoundException}, {@link BusinessRuleException}
 * etc.). This class is kept for backwards compatibility and as a last-resort
 * generic 400 carrier.
 *
 * @deprecated extend {@link ApiException} or one of its named subclasses
 *     instead of this one.
 */
@Deprecated
public class BusinessException extends ApiException {

    public BusinessException(String message) {
        super(HttpStatus.BAD_REQUEST, "BUSINESS_ERROR", message);
    }

    public BusinessException(HttpStatus status, String code, String message) {
        super(status, code, message);
    }

    public BusinessException(HttpStatus status, String code, String message, Throwable cause) {
        super(status, code, message, cause);
    }
}