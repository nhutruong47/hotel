package com.hsf.hotel.payment.dto;

import com.hsf.hotel.payment.model.Payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * API view of a payment (P0-04). Omits the gateway {@code rawResponse}, the
 * Stripe {@code intentId} and the processing user, which the entity used to leak.
 * Must be built inside a transaction (booking is a lazy association).
 */
public record PaymentResponse(
        Integer id,
        BookingRef booking,
        BigDecimal amount,
        String currency,
        String method,
        String status,
        String gateway,
        String transactionRef,
        BigDecimal refundAmount,
        String refundReason,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime completedAt,
        LocalDateTime refundedAt,
        Boolean reviewRequired,
        String reviewReason
) {
    public record BookingRef(Integer id, String guestName, String status) {}

    public static PaymentResponse from(Payment p) {
        if (p == null) return null;
        BookingRef booking = p.getBooking() == null ? null : new BookingRef(
                p.getBooking().getId(),
                p.getBooking().getGuestName(),
                p.getBooking().getStatus() != null ? p.getBooking().getStatus().name() : null);
        return new PaymentResponse(
                p.getId(),
                booking,
                p.getAmount(),
                p.getCurrency(),
                p.getMethod() != null ? p.getMethod().name() : null,
                p.getStatus() != null ? p.getStatus().name() : null,
                p.getGateway() != null ? p.getGateway().name() : null,
                p.getTransactionRef(),
                p.getRefundAmount(),
                p.getRefundReason(),
                p.getNotes(),
                p.getCreatedAt(),
                p.getCompletedAt(),
                p.getRefundedAt(),
                Boolean.TRUE.equals(p.getReviewRequired()),
                p.getReviewReason()
        );
    }
}
