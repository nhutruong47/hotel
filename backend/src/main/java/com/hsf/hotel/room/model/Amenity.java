package com.hsf.hotel.room.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "amenities")
@jakarta.persistence.EntityListeners(com.hsf.hotel.common.audit.AuditStampListener.class)
public class Amenity implements java.io.Serializable, com.hsf.hotel.common.audit.AuditStamped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column
    private String iconCode; // e.g. "fa-wifi", "fa-swimming-pool", "📺", "📶"

    public Amenity() {
    }

    public Amenity(String name, String iconCode) {
        this.name = name;
        this.iconCode = iconCode;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIconCode() {
        return iconCode;
    }

    public void setIconCode(String iconCode) {
        this.iconCode = iconCode;
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
    public Integer getUpdatedBy() { return updatedBy; }}
