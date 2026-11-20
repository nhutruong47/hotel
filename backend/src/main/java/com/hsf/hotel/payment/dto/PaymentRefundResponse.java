package com.hsf.hotel.payment.dto;

import com.hsf.hotel.booking.model.Booking;
import com.hsf.hotel.payment.model.PaymentRefund;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Refund view for ADMIN tooling. The guest's refund bank details are included
 * only when {@code includeBankDetails} is set — i.e. on the ADMIN-only manual
 * refund work queue, where they are needed to make the transfer (need-to-know).
 * Must be built inside a transaction (lazy payment/booking/users).
 */
public record PaymentRefundResponse(
        Long id,
        Integer paymentId,
        Integer bookingId,
        String guestName,
        BigDecimal amount,
        String reason,
        String status,
        String requestedBy,
        LocalDateTime createdAt,
        LocalDateTime completedAt,
        String bankReference,
        String confirmedBy,
        LocalDateTime confirmedAt,
        BankDetails refundTo
) {
    public record BankDetails(String bankName, String accountNumber, String accountName) {}

    public static PaymentRefundResponse from(PaymentRefund r, boolean includeBankDetails) {
        if (r == null) return null;
        Booking b = r.getPayment() != null ? r.getPayment().getBooking() : null;
        BankDetails bank = includeBankDetails && b != null && b.getRefundAccountNumber() != null
                ? new BankDetails(b.getRefundBankName(), b.getRefundAccountNumber(), b.getRefundAccountName())
                : null;
        return new PaymentRefundResponse(
                r.getId(),
                r.getPayment() != null ? r.getPayment().getId() : null,
                b != null ? b.getId() : null,
                b != null ? b.getGuestName() : null,
                r.getAmount(),
                r.getReason(),
                r.getStatus() != null ? r.getStatus().name() : null,
                r.getProcessedBy() != null ? r.getProcessedBy().getUsername() : null,
                r.getCreatedAt(),
                r.getCompletedAt(),
                r.getBankReference(),
                r.getConfirmedBy() != null ? r.getConfirmedBy().getUsername() : null,
                r.getConfirmedAt(),
                bank
        );
    }
}
