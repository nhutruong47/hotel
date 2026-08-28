package com.hsf.hotel.exception;

import org.springframework.http.HttpStatus;

public class InventoryException extends BusinessException {
    public InventoryException(String message) {
        super(HttpStatus.CONFLICT, "INVENTORY_ERROR", message);
    }
    
    public InventoryException(String message, Throwable cause) {
        super(HttpStatus.CONFLICT, "INVENTORY_ERROR", message, cause);
    }
}
