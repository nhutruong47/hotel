package com.hsf.hotel.exception;

import org.springframework.http.HttpStatus;

public class FileStorageException extends BusinessException {
    public FileStorageException(String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, "FILE_STORAGE_ERROR", message);
    }
    
    public FileStorageException(String message, Throwable cause) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, "FILE_STORAGE_ERROR", message, cause);
    }
}
