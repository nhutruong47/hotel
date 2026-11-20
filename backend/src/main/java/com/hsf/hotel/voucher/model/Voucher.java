package com.hsf.hotel.voucher.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "vouchers", indexes = {
        @Index(name = "idx_vouchers_code", columnList = "code", unique = true),
        @Index(name = "idx_vouchers_expiry", columnList = "expiryDate")
})
@jakarta.persistence.EntityListeners(com.hsf.hotel.common.audit.AuditStampListener.class)
public class Voucher implements com.hsf.hotel.common.audit.AuditStamped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String code;

    // amount meaning:
    // - if percent = false -> fixed amount in currency to subtract
    // - if percent = true -> percentage value (e.g., 10 means 10%)
    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal amount;

    private LocalDate expiryDate;

    @Column(nullable = false)
    private Integer quantity = 1; // remaining uses

    @Transient
    private Integer usedCount = 0;

    // If true, 'amount' is treated as percentage (0-100); otherwise 'amount' is fixed currency
    @Column(name = "is_percent", nullable = false)
    private Boolean percent = false;

    public Voucher() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getUsedCount() {
        return usedCount;
    }

    public void setUsedCount(Integer usedCount) {
        this.usedCount = usedCount;
    }

    public Boolean getPercent() {
        return percent;
    }

    public void setPercent(Boolean percent) {
        this.percent = percent;
    }

    // ---------------- audit columns (V19, AuditStampListener) ----------------
    @jakarta.persistence.Column(name = "created_at", nullable = false, updatable = false)
    private java.time.LocalDateTime createdAt;

    @jakarta.persistence.Column(name = "updated_at")
    private java.time.LocalDateTime updatedAt;

    @jakarta.persistence.Column(name = "created_by", updatable = false)
    private Integer createdBy;

    @jakarta.persistence.Column(name = "updated_by")
    private Integer updatedBy;

    @Override
    public void onCreateStamp(java.time.LocalDateTime now, Integer actorUserId) {
        if (createdAt == null) createdAt = now;
        if (createdBy == null) createdBy = actorUserId;
    }

    @Override
    public void onUpdateStamp(java.time.LocalDateTime now, Integer actorUserId) {
        updatedAt = now;
        updatedBy = actorUserId;
    }

    public java.time.LocalDateTime getCreatedAt() { return createdAt; }
    public java.time.LocalDateTime getUpdatedAt() { return updatedAt; }
    public Integer getCreatedBy() { return createdBy; }
    public Integer getUpdatedBy() { return updatedBy; }

    /** V20: null = valid at every property. */
    @jakarta.persistence.Column(name = "property_id")
    private Integer propertyId;

    public Integer getPropertyId() { return propertyId; }
    public void setPropertyId(Integer propertyId) { this.propertyId = propertyId; }
}
