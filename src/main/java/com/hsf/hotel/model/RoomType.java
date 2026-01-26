package com.hsf.hotel.model;

public enum RoomType {
    STANDARD("Phòng Standard", "Phòng đơn tiêu chuẩn, đầy đủ tiện nghi cơ bản"),
    DELUXE("Phòng Deluxe", "Phòng cao cấp với view đẹp và tiện nghi hiện đại"),
    SUITE("Phòng Suite", "Phòng suite rộng rãi với phòng khách riêng"),
    VIP("Phòng VIP", "Phòng VIP sang trọng nhất với dịch vụ đặc biệt");

    private final String displayName;
    private final String description;

    RoomType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
