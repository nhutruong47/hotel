package com.hsf.hotel.review.dto;

import com.hsf.hotel.booking.model.Booking;
import com.hsf.hotel.room.model.Room;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ReviewDetailResponse {

    public record BookingSummary(
            Integer id,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            BigDecimal totalPrice,
            String status
    ) {
        public static BookingSummary from(Booking b) {
            if (b == null) return null;
            return new BookingSummary(
                    b.getId(),
                    b.getCheckInDate(),
                    b.getCheckOutDate(),
                    b.getTotalPrice(),
                    b.getStatus() != null ? b.getStatus().name() : null
            );
        }
    }

    public record RoomSummary(
            Integer id,
            String roomNumber,
            String slug,
            String imageUrl
    ) {
        public static RoomSummary from(Room r) {
            if (r == null) return null;
            return new RoomSummary(
                    r.getId(),
                    r.getRoomNumber(),
                    r.getSlug(),
                    r.getImageUrl()
            );
        }
    }

    public record BookingReviewContext(
            BookingSummary booking,
            RoomSummary room,
            PublicReviewResponse existing,
            boolean isEdit
    ) {}
}
