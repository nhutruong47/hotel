package com.hsf.hotel.config;

/**
 * Stable error codes returned by the REST API. Frontend should map these
 * values, not message text, when switching on an error. New codes MUST be
 * added here rather than embedded as raw strings in throw statements.
 */
public final class ErrorCodes {
    private ErrorCodes() {}

    // ---- Generic / HTTP-aligned ----
    public static final String VALIDATION_FAILED = "VALIDATION_FAILED";
    public static final String BAD_REQUEST = "BAD_REQUEST";
    public static final String UNAUTHORIZED = "UNAUTHORIZED";
    public static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
    public static final String FORBIDDEN = "FORBIDDEN";
    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String CONFLICT = "CONFLICT";
    public static final String CONCURRENCY_CONFLICT = "CONCURRENCY_CONFLICT";
    public static final String RATE_LIMIT_EXCEEDED = "RATE_LIMIT_EXCEEDED";
    public static final String FILE_TOO_LARGE = "FILE_TOO_LARGE";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
    public static final String EXTERNAL_SERVICE_ERROR = "EXTERNAL_SERVICE_ERROR";
    public static final String FILE_STORAGE_ERROR = "FILE_STORAGE_ERROR";
    public static final String DATABASE_ERROR = "DATABASE_ERROR";
    public static final String DUPLICATE_RESOURCE = "DUPLICATE_RESOURCE";
    public static final String CSRF_TOKEN_INVALID = "CSRF_TOKEN_INVALID";
    public static final String METHOD_NOT_ALLOWED = "METHOD_NOT_ALLOWED";
    public static final String UNSUPPORTED_MEDIA_TYPE = "UNSUPPORTED_MEDIA_TYPE";

    // ---- Domain-specific ----
    public static final String BOOKING_NOT_FOUND = "BOOKING_NOT_FOUND";
    public static final String BOOKING_ERROR = "BOOKING_ERROR";
    public static final String ROOM_UNAVAILABLE = "ROOM_UNAVAILABLE";
    public static final String INVENTORY_ERROR = "INVENTORY_ERROR";
    public static final String INVALID_VOUCHER = "INVALID_VOUCHER";
    public static final String VOUCHER_ERROR = "VOUCHER_ERROR";
    public static final String INVALID_BOOKING_STATE = "INVALID_BOOKING_STATE";
    public static final String PAYMENT_ERROR = "PAYMENT_ERROR";
    public static final String CAPTCHA_INVALID = "CAPTCHA_INVALID";
    public static final String EMAIL_EXISTS = "EMAIL_EXISTS";
    public static final String USERNAME_EXISTS = "USERNAME_EXISTS";
    public static final String PASSWORD_MISMATCH = "PASSWORD_MISMATCH";
    public static final String WEAK_PASSWORD = "WEAK_PASSWORD";
    public static final String INVALID_USERNAME = "INVALID_USERNAME";
    public static final String INVALID_EMAIL = "INVALID_EMAIL";
    public static final String INVALID_PASSWORD = "INVALID_PASSWORD";
    public static final String USERNAME_TAKEN = "USERNAME_TAKEN";
    public static final String EMAIL_TAKEN = "EMAIL_TAKEN";
    public static final String ALREADY_VERIFIED = "ALREADY_VERIFIED";
    public static final String INVALID_TOKEN = "INVALID_TOKEN";
    public static final String LOCKED = "LOCKED";
    public static final String RESET_FAILED = "RESET_FAILED";
    public static final String UPDATE_FAILED = "UPDATE_FAILED";
    public static final String INVALID_PREFERENCES = "INVALID_PREFERENCES";
    public static final String BUSINESS_ERROR = "BUSINESS_ERROR";
    public static final String RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";
}
