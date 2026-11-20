package com.hsf.hotel.booking.dto;

import com.hsf.hotel.booking.model.BookingStatusTransition;

import java.time.LocalDateTime;

/** One row of a booking's status timeline (replaces the serialized entity, P0-04). */
public record BookingTransitionResponse(
        Integer id,
        String fromStatus,
        String toStatus,
        String reason,
        String changedBy,
        LocalDateTime createdAt
) {
    public static BookingTransitionResponse from(BookingStatusTransition t) {
        if (t == null) return null;
        return new BookingTransitionResponse(
                t.getId(),
                t.getFromStatus() != null ? t.getFromStatus().name() : null,
                t.getToStatus() != null ? t.getToStatus().name() : null,
                t.getReason(),
                t.getUser() != null ? t.getUser().getUsername() : null,
                t.getCreatedAt()
        );
    }
}
