package com.hsf.hotel.model;

public enum BookingStatus {
    PENDING_PAYMENT("Pending Payment"),
    PAID("Paid"),
    AWAITING_APPROVAL("Awaiting Approval"),
    CONFIRMED("Confirmed"),
    CHECKED_IN("Checked In"),
    CHECKED_OUT("Checked Out"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled"),
    REJECTED("Rejected"),
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
