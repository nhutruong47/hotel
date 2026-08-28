package com.hsf.hotel.exception;

import org.springframework.http.HttpStatus;

public class BookingException extends BusinessException {
    public BookingException(String message) {
        super(HttpStatus.BAD_REQUEST, "BOOKING_ERROR", message);
    }
    
    public BookingException(String message, Throwable cause) {
        super(HttpStatus.BAD_REQUEST, "BOOKING_ERROR", message, cause);
    }
}
