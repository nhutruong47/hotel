package com.hsf.hotel.notification.model;
import com.hsf.hotel.promotion.model.Promotion;
import com.hsf.hotel.payment.model.Payment;
import com.hsf.hotel.notification.model.Notification;
import com.hsf.hotel.booking.model.Booking;

/**
 * Category of in-app {@link Notification} records. Backed in the database as
 * a VARCHAR via {@link Notification#setType(String)} but constrained at the
 * Java layer to a known set so consumers (UI, email templates) cannot get
 * typo-spawned categories.
 */
public enum NotificationType {
    SYSTEM("System"),
    BOOKING("Booking"),
    PAYMENT("Payment"),
    REFUND("Refund"),
    PROMOTION("Promotion");

    private final String displayName;

    NotificationType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}