package com.hsf.hotel.security;
import com.hsf.hotel.admin.model.AuditLog;

/**
 * Centralised list of {@code AuditLog.action} values so audit messages
 * cannot drift across services. Add new constants here rather than passing
 * raw strings to {@link com.hsf.hotel.admin.service.AuditLogService}.
 */
public final class AuditActions {
    private AuditActions() {}

    // Authentication
    public static final String LOGIN_SUCCESS = "LOGIN_SUCCESS";
    public static final String LOGIN_FAILED = "LOGIN_FAILED";
    public static final String LOGOUT = "LOGOUT";
    public static final String REGISTER = "REGISTER";
    public static final String PASSWORD_RESET_REQUEST = "PASSWORD_RESET_REQUEST";
    public static final String PASSWORD_RESET_COMPLETE = "PASSWORD_RESET_COMPLETE";
    public static final String EMAIL_VERIFIED = "EMAIL_VERIFIED";

    // Profile
    public static final String PROFILE_UPDATE = "PROFILE_UPDATE";
    public static final String PASSWORD_CHANGE = "PASSWORD_CHANGE";

    // Bookings
    public static final String BOOKING_CREATE = "BOOKING_CREATE";
    public static final String BOOKING_MODIFY = "BOOKING_MODIFY";
    public static final String BOOKING_CANCEL = "BOOKING_CANCEL";
    public static final String BOOKING_CHECKIN = "BOOKING_CHECKIN";
    public static final String BOOKING_CHECKOUT = "BOOKING_CHECKOUT";
    public static final String BOOKING_COMPLETE = "BOOKING_COMPLETE";
    public static final String BOOKING_NO_SHOW = "BOOKING_NO_SHOW";

    // Payments
    public static final String PAYMENT_CREATE = "PAYMENT_CREATE";
    public static final String PAYMENT_CONFIRM = "PAYMENT_CONFIRM";
    public static final String PAYMENT_FAIL = "PAYMENT_FAIL";
    public static final String PAYMENT_REFUND = "PAYMENT_REFUND";
    public static final String PAYMENT_WEBHOOK_RECEIVED = "PAYMENT_WEBHOOK_RECEIVED";

    // Admin actions
    public static final String ADMIN_USER_CREATE = "ADMIN_USER_CREATE";
    public static final String ADMIN_USER_ROLE_CHANGE = "ADMIN_USER_ROLE_CHANGE";
    public static final String ADMIN_USER_DELETE = "ADMIN_USER_DELETE";
    public static final String ADMIN_USER_DISABLE = "ADMIN_USER_DISABLE";
    public static final String ADMIN_ROOM_CHANGE = "ADMIN_ROOM_CHANGE";
    public static final String ADMIN_VOUCHER_CHANGE = "ADMIN_VOUCHER_CHANGE";
    public static final String ADMIN_PROMOTION_CHANGE = "ADMIN_PROMOTION_CHANGE";
    public static final String ADMIN_REVIEW_MODERATE = "ADMIN_REVIEW_MODERATE";
    public static final String ADMIN_REFUND_ISSUE = "ADMIN_REFUND_ISSUE";
    public static final String ADMIN_REPORT_EXPORT = "ADMIN_REPORT_EXPORT";

    // Security
    public static final String RATE_LIMIT_EXCEEDED = "RATE_LIMIT_EXCEEDED";
    public static final String UNAUTHORIZED_ACCESS_ATTEMPT = "UNAUTHORIZED_ACCESS_ATTEMPT";
    public static final String CSRF_REJECTED = "CSRF_REJECTED";
}
