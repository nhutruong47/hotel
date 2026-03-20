package com.hsf.hotel.model;

public enum BookingStatus {
    CONFIRMED("Đã xác nhận"),
    CANCELLED("Đã hủy"),
    COMPLETED("Hoàn thành"),
    AWAITING_PAYMENT("Chờ thanh toán"),
    AWAITING_APPROVAL("Chờ duyệt"),
    PENDING("Chờ xử lý"),
    REJECTED("Từ chối");

    private final String displayName;

    BookingStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
