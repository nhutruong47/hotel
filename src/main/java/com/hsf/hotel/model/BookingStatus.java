package com.hsf.hotel.model;

public enum BookingStatus {
    PENDING("Chờ xác nhận"),
    AWAITING_PAYMENT("Chờ thanh toán"),
    CONFIRMED("Đã xác nhận"),
    REJECTED("Bị từ chối"),
    CANCELLED("Đã hủy"),
    COMPLETED("Hoàn thành");

    private final String displayName;

    BookingStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
