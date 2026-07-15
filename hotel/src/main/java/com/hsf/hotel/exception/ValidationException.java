package com.hsf.hotel.exception;

import org.springframework.http.HttpStatus;

public class ValidationException extends BusinessException {
    public ValidationException(String message) {
        super(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }
    
    public ValidationException(String message, Throwable cause) {
        super(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, cause);
    }
}
