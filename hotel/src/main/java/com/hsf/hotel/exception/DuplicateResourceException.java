package com.hsf.hotel.exception;

import org.springframework.http.HttpStatus;

public class DuplicateResourceException extends BusinessException {
    public DuplicateResourceException(String message) {
        super(HttpStatus.CONFLICT, "DUPLICATE_RESOURCE", message);
    }
    
    public DuplicateResourceException(String message, Throwable cause) {
        super(HttpStatus.CONFLICT, "DUPLICATE_RESOURCE", message, cause);
    }
}
