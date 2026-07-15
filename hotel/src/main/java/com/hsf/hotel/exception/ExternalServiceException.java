package com.hsf.hotel.exception;

import org.springframework.http.HttpStatus;

public class ExternalServiceException extends BusinessException {
    public ExternalServiceException(String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "EXTERNAL_SERVICE_ERROR", message);
    }
    
    public ExternalServiceException(String message, Throwable cause) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "EXTERNAL_SERVICE_ERROR", message, cause);
    }
}
