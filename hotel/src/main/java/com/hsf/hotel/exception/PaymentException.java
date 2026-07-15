package com.hsf.hotel.exception;

import org.springframework.http.HttpStatus;

public class PaymentException extends BusinessException {
    public PaymentException(String message) {
        super(HttpStatus.BAD_REQUEST, "PAYMENT_ERROR", message);
    }
    
    public PaymentException(String message, Throwable cause) {
        super(HttpStatus.BAD_REQUEST, "PAYMENT_ERROR", message, cause);
    }
}
