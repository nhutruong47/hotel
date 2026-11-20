package com.hsf.hotel.room.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "room_types")
@jakarta.persistence.EntityListeners(com.hsf.hotel.common.audit.AuditStampListener.class)
public class RoomTypeEntity implements java.io.Serializable, com.hsf.hotel.common.audit.AuditStamped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    // We can map back to rooms if needed, but for now just the basic fields

    public RoomTypeEntity() {
    }

    public RoomTypeEntity(String name, String description) {
        this.name = name;
        this.description = description;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDisplayName() {
        return this.name;
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
