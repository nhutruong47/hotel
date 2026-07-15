package com.hsf.hotel.model;

public enum BookingStatus {
    PENDING_PAYMENT("Pending Payment"),
    PAID("Paid"),
    CHECKED_IN("Checked In"),
    CHECKED_OUT("Checked Out"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled"),
    EXPIRED("Expired"),
    NO_SHOW("No Show");

    private final String displayName;

    BookingStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
