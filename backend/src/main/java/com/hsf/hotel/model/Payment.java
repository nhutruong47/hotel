package com.hsf.hotel.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payments_booking", columnList = "booking_id"),
        @Index(name = "idx_payments_status", columnList = "status"),
        @Index(name = "idx_payments_transaction", columnList = "transactionRef"),
        @Index(name = "idx_payments_processed_by", columnList = "processed_by"),
        @Index(name = "idx_payments_created", columnList = "createdAt")
})
public class Payment {

    public enum PaymentStatus {
        PENDING("Pending"),
        PAID("Paid"),
        FAILED("Failed"),
        REFUNDED("Refunded"),
        PARTIALLY_REFUNDED("Partially Refunded");

        private final String displayName;
        PaymentStatus(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    public enum PaymentGateway {
        STRIPE("Stripe"),
        VNPAY("VNPay"),
        MOCK("Mock Gateway");

        private final String displayName;
        PaymentGateway(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    public enum PaymentMethod {
        BANK_TRANSFER("Chuyển khoản ngân hàng"),
        QR_CODE("QR Code"),
        CASH("Tiền mặt"),
        CARD("Thẻ tín dụng"),
        OTHER("Khác");

        private final String displayName;
        PaymentMethod(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod method = PaymentMethod.BANK_TRANSFER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    private PaymentGateway gateway;

    @Column(nullable = false, length = 3)
    private String currency = "VND";

    @Column(unique = true, length = 100)
    private String transactionRef;

    @Column(name = "intent_id", unique = true)
    private String intentId;

    @Column(name = "raw_response", columnDefinition = "TEXT")
    private String rawResponse;

    @Column(length = 500)
    private String notes;

    @Column(precision = 12, scale = 0)
    private BigDecimal refundAmount = BigDecimal.ZERO;

    @Column(length = 500)
    private String refundReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by")
    private User processedBy;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime completedAt;

    private LocalDateTime refundedAt;

    public Payment() {}

    public Payment(Booking booking, BigDecimal amount, PaymentMethod method, PaymentStatus status, String transactionRef) {
        this.booking = booking;
        this.amount = amount;
        this.method = method;
        this.status = status;
        this.transactionRef = transactionRef;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public PaymentMethod getMethod() { return method; }
    public void setMethod(PaymentMethod method) { this.method = method; }
    public PaymentMethod getPaymentMethod() { return method; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.method = paymentMethod; }
    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }
    public String getTransactionRef() { return transactionRef; }
    public void setTransactionRef(String transactionRef) { this.transactionRef = transactionRef; }
    public String getPaymentRef() { return transactionRef; }
    public void setPaymentRef(String paymentRef) { this.transactionRef = paymentRef; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(BigDecimal refundAmount) {
        // Reject negative or non-finite refund amounts at the model layer so a
        // single refund call cannot accidentally drive the running total below
        // zero (the service layer also enforces refund <= amount, but a
        // defensive check here keeps the entity self-consistent).
        if (refundAmount != null && refundAmount.signum() < 0) {
            throw new IllegalArgumentException("refundAmount cannot be negative");
        }
        this.refundAmount = refundAmount;
    }
    public String getRefundReason() { return refundReason; }
    public void setRefundReason(String refundReason) { this.refundReason = refundReason; }
    public User getProcessedBy() { return processedBy; }
    public void setProcessedBy(User processedBy) { this.processedBy = processedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public LocalDateTime getRefundedAt() { return refundedAt; }
    public void setRefundedAt(LocalDateTime refundedAt) { this.refundedAt = refundedAt; }
    public PaymentGateway getGateway() { return gateway; }
    public void setGateway(PaymentGateway gateway) { this.gateway = gateway; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getIntentId() { return intentId; }
    public void setIntentId(String intentId) { this.intentId = intentId; }
    public String getRawResponse() { return rawResponse; }
    public void setRawResponse(String rawResponse) { this.rawResponse = rawResponse; }
}
