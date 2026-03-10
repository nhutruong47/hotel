package com.hsf.hotel.model;

public enum BookingStatus {
    PENDING("Pending"),
    AWAITING_PAYMENT("Awaiting Payment"),
    CONFIRMED("Confirmed"),
    REJECTED("Rejected"),
    CANCELLED("Cancelled"),
    COMPLETED("Completed");

    private final String displayName;

    BookingStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
