package com.hsf.hotel.room.dto;

public interface RoomStatsDto {
    Integer getRoomId();
    String getRoomNumber();
    Long getTotalBookings();
    Double getAverageRating();
}
