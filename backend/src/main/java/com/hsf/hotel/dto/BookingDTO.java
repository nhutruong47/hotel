package com.hsf.hotel.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class BookingDTO {
    private Integer id;
    private Long version;
    private UserSummaryDTO user;
    private RoomDTO room;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private String status;
    private String statusDisplayName;
    private BigDecimal totalPrice;
    private BigDecimal subtotalPrice;
    private BigDecimal serviceFee;
    private BigDecimal taxAmount;
    private BigDecimal refundAmount;
    private Integer refundPercentage;
    private String guestName;
    private String guestPhone;
    private String guestEmail;
    private Integer guests;
    private String notes;
    private String cancellationReason;
    private String appliedVoucherCode;
    private BigDecimal discountAmount;
    private UserSummaryDTO approvedBy;
    private LocalDateTime approvedAt;
    private String rejectionReason;
    private LocalDateTime paymentDeadline;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;
    private LocalDateTime checkedInAt;
    private LocalDateTime checkedOutAt;
    private LocalDateTime holdExpiresAt;
    private LocalDateTime cancelledAt;
    private String cancelledBy;
}
