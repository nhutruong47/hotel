package com.hsf.hotel.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends BusinessException {
    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, "CONFLICT", message);
    }
    
    public ConflictException(String message, Throwable cause) {
        super(HttpStatus.CONFLICT, "CONFLICT", message, cause);
    }
}
