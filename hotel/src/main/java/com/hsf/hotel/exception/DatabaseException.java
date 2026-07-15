package com.hsf.hotel.exception;

import org.springframework.http.HttpStatus;

public class DatabaseException extends BusinessException {
    public DatabaseException(String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, "DATABASE_ERROR", message);
    }
    
    public DatabaseException(String message, Throwable cause) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, "DATABASE_ERROR", message, cause);
    }
}
