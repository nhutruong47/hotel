package com.hsf.hotel.model;

public enum RoomType {
    STANDARD("Standard Room", "Standard single room with basic amenities"),
    DELUXE("Deluxe Room", "Premium room with beautiful view and modern amenities"),
    SUITE("Suite Room", "Spacious suite with a private living room"),
    VIP("VIP Room", "The most luxurious VIP room with exclusive services");

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
