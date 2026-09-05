package com.hsf.hotel.booking.model;
import com.hsf.hotel.user.model.User;
import com.hsf.hotel.room.model.Room;
import com.hsf.hotel.booking.model.BookingStatus;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "bookings", indexes = {
        @Index(name = "idx_bookings_room_dates", columnList = "room_id, checkInDate, checkOutDate"),
        @Index(name = "idx_bookings_user", columnList = "user_id"),
        @Index(name = "idx_bookings_status", columnList = "status"),
        @Index(name = "idx_bookings_created", columnList = "createdAt"),
        @Index(name = "idx_bookings_checkin_date", columnList = "checkInDate"),
        @Index(name = "idx_bookings_checkout_date", columnList = "checkOutDate"),
        @Index(name = "idx_bookings_approved_by", columnList = "approved_by"),
        @Index(name = "idx_bookings_payment_deadline", columnList = "paymentDeadline"),
        @Index(name = "idx_bookings_hold_expires", columnList = "holdExpiresAt")
})
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(nullable = false)
    private LocalDate checkInDate;

    @Column(nullable = false)
    private LocalDate checkOutDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status = BookingStatus.PENDING_PAYMENT;

    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal totalPrice;

    /** Base price before fees and discounts (nights × nightly rate). */
    @Column(precision = 12, scale = 0)
    private BigDecimal subtotalPrice;

    /** Service preparation fee (typically 8% of subtotal). */
    @Column(precision = 12, scale = 0)
    private BigDecimal serviceFee = BigDecimal.ZERO;

    /** Tax amount (typically 10% VAT). */
    @Column(precision = 12, scale = 0)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(precision = 12, scale = 0)
    private BigDecimal refundAmount;

    @Column
    private Integer refundPercentage;

    private String guestName;
    private String guestPhone;
    private String guestEmail;
    private Integer guests;
    private String notes;
    private String cancellationReason;

    // Store applied voucher code (if any) and discount amount
    @Column(name = "applied_voucher_code")
    private String appliedVoucherCode;

    @Column(name = "discount_amount", precision = 12, scale = 0)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    // Workflow fields
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    private LocalDateTime approvedAt;
    private String rejectionReason;
    @Column(nullable = false)
    private LocalDateTime paymentDeadline;
    private LocalDateTime paidAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime modifiedAt;
    private LocalDateTime checkedInAt;
    private LocalDateTime checkedOutAt;
    private LocalDateTime holdExpiresAt;

    private LocalDateTime cancelledAt;
    @Column(length = 64)
    private String cancelledBy;

    public Booking() {
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public LocalDate getCheckInDate() {
        return checkInDate;
    }

    public void setCheckInDate(LocalDate checkInDate) {
        this.checkInDate = checkInDate;
    }

    public LocalDate getCheckOutDate() {
        return checkOutDate;
    }

    public void setCheckOutDate(LocalDate checkOutDate) {
        this.checkOutDate = checkOutDate;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getGuestName() {
        return guestName;
    }

    public void setGuestName(String guestName) {
        this.guestName = guestName;
    }

    public String getGuestPhone() {
        return guestPhone;
    }

    public void setGuestPhone(String guestPhone) {
        this.guestPhone = guestPhone;
    }

    public String getGuestEmail() {
        return guestEmail;
    }

    public void setGuestEmail(String guestEmail) {
        this.guestEmail = guestEmail;
    }

    public Integer getGuests() {
        return guests;
    }

    public void setGuests(Integer guests) {
        this.guests = guests;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public BigDecimal getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(BigDecimal refundAmount) {
        this.refundAmount = refundAmount;
    }

    public Integer getRefundPercentage() {
        return refundPercentage;
    }

    public void setRefundPercentage(Integer refundPercentage) {
        this.refundPercentage = refundPercentage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getHoldExpiresAt() {
        return holdExpiresAt;
    }

    public void setHoldExpiresAt(LocalDateTime holdExpiresAt) {
        this.holdExpiresAt = holdExpiresAt;
    }

    // Getter/setter for voucher fields
    public String getAppliedVoucherCode() {
        return appliedVoucherCode;
    }

    public void setAppliedVoucherCode(String appliedVoucherCode) {
        this.appliedVoucherCode = appliedVoucherCode;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    // Helper methods
    public long getNumberOfNights() {
        if (checkInDate != null && checkOutDate != null) {
            return ChronoUnit.DAYS.between(checkInDate, checkOutDate);
        }
        return 0;
    }

    public String getStatusDisplayName() {
        return status != null ? status.getDisplayName() : "";
    }

    // --- Pricing breakdown getters/setters ---
    public BigDecimal getSubtotalPrice() { return subtotalPrice; }
    public void setSubtotalPrice(BigDecimal subtotalPrice) { this.subtotalPrice = subtotalPrice; }
    public BigDecimal getServiceFee() { return serviceFee; }
    public void setServiceFee(BigDecimal serviceFee) { this.serviceFee = serviceFee; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }

    // --- Workflow getters/setters ---
    public User getApprovedBy() { return approvedBy; }
    public void setApprovedBy(User approvedBy) { this.approvedBy = approvedBy; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public LocalDateTime getPaymentDeadline() { return paymentDeadline; }
    public void setPaymentDeadline(LocalDateTime paymentDeadline) { this.paymentDeadline = paymentDeadline; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }

    // --- Operational timestamps ---
    public LocalDateTime getModifiedAt() { return modifiedAt; }
    public void setModifiedAt(LocalDateTime modifiedAt) { this.modifiedAt = modifiedAt; }
    public LocalDateTime getCheckedInAt() { return checkedInAt; }
    public void setCheckedInAt(LocalDateTime checkedInAt) { this.checkedInAt = checkedInAt; }
    public LocalDateTime getCheckedOutAt() { return checkedOutAt; }
    public void setCheckedOutAt(LocalDateTime checkedOutAt) { this.checkedOutAt = checkedOutAt; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
    public String getCancelledBy() { return cancelledBy; }
    public void setCancelledBy(String cancelledBy) { this.cancelledBy = cancelledBy; }

    /**
     * Ensure {@code createdAt} is set from the server clock at insert time, and
     * refresh {@code modifiedAt} on every update. Field initializers (which
     * capture {@code new Booking()} time) drift when instances are reused
     * across long-running transactions or constructed before persistence; the
     * {@code @PrePersist} handler is the authoritative source.
     */
    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        modifiedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        modifiedAt = LocalDateTime.now();
    }
}
