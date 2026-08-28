package com.hsf.hotel.dto;

public interface RoomStatsDto {
    Integer getRoomId();
    String getRoomNumber();
    Long getTotalBookings();
    Double getAverageRating();
}
