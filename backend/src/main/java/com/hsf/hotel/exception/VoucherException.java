package com.hsf.hotel.exception;

import org.springframework.http.HttpStatus;

public class VoucherException extends BusinessException {
    public VoucherException(String message) {
        super(HttpStatus.BAD_REQUEST, "VOUCHER_ERROR", message);
    }
    
    public VoucherException(String message, Throwable cause) {
        super(HttpStatus.BAD_REQUEST, "VOUCHER_ERROR", message, cause);
    }
}
