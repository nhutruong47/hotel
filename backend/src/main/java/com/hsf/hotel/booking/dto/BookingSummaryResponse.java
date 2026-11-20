package com.hsf.hotel.booking.dto;

import com.hsf.hotel.booking.model.Booking;
import com.hsf.hotel.room.model.Room;
import com.hsf.hotel.user.model.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Staff/back-office view of a booking. Replaces direct serialization of the
 * {@link Booking} entity (P0-04): no lazy proxies, no refund bank account data,
 * no approver/user internals. Field names match the previous entity JSON so the
 * admin SPA keeps working. Must be built inside a transaction.
 */
public record BookingSummaryResponse(
        Integer id,
        Long version,
        String status,
        String statusDisplayName,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        String guestName,
        String guestEmail,
        String guestPhone,
        Integer guests,
        String notes,
        BigDecimal totalPrice,
        BigDecimal subtotalPrice,
        BigDecimal serviceFee,
        BigDecimal taxAmount,
        BigDecimal discountAmount,
        String appliedVoucherCode,
        BigDecimal refundAmount,
        Integer refundPercentage,
        String cancellationReason,
        LocalDateTime cancelledAt,
        String cancelledBy,
        LocalDateTime paymentDeadline,
        LocalDateTime holdExpiresAt,
        LocalDateTime paidAt,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt,
        LocalDateTime checkedInAt,
        LocalDateTime checkedOutAt,
        RoomRef room,
        UserRef user
) {

    public record RoomRef(Integer id, String roomNumber, String slug, String imageUrl) {
        static RoomRef from(Room r) {
            return r == null ? null : new RoomRef(r.getId(), r.getRoomNumber(), r.getSlug(), r.getImageUrl());
        }
    }

    public record UserRef(Integer id, String username, String email, String fullName) {
        static UserRef from(User u) {
            return u == null ? null : new UserRef(u.getId(), u.getUsername(), u.getEmail(), u.getFullName());
        }
    }

    public static BookingSummaryResponse from(Booking b) {
        if (b == null) return null;
        return new BookingSummaryResponse(
                b.getId(),
                b.getVersion(),
                b.getStatus() != null ? b.getStatus().name() : null,
                b.getStatus() != null ? b.getStatus().getDisplayName() : null,
                b.getCheckInDate(),
                b.getCheckOutDate(),
                b.getGuestName(),
                b.getGuestEmail(),
                b.getGuestPhone(),
                b.getGuests(),
                b.getNotes(),
                b.getTotalPrice(),
                b.getSubtotalPrice(),
                b.getServiceFee(),
                b.getTaxAmount(),
                b.getDiscountAmount(),
                b.getAppliedVoucherCode(),
                b.getRefundAmount(),
                b.getRefundPercentage(),
                b.getCancellationReason(),
                b.getCancelledAt(),
                b.getCancelledBy(),
                b.getPaymentDeadline(),
                b.getHoldExpiresAt(),
                b.getPaidAt(),
                b.getCreatedAt(),
                b.getModifiedAt(),
                b.getCheckedInAt(),
                b.getCheckedOutAt(),
                RoomRef.from(b.getRoom()),
                UserRef.from(b.getUser())
        );
    }
}
