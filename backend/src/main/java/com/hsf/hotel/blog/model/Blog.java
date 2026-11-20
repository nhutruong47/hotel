package com.hsf.hotel.blog.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "blogs")
@Data
@jakarta.persistence.EntityListeners(com.hsf.hotel.common.audit.AuditStampListener.class)
public class Blog implements com.hsf.hotel.common.audit.AuditStamped {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String excerpt;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String imageUrl;
    
    private String author;

    private LocalDateTime createdAt = LocalDateTime.now();

    // ---------------- audit columns (V19, AuditStampListener) ----------------
    @jakarta.persistence.Column(name = "updated_at")
    private java.time.LocalDateTime updatedAt;

    @jakarta.persistence.Column(name = "created_by", updatable = false)
    private Integer createdBy;

    @jakarta.persistence.Column(name = "updated_by")
    private Integer updatedBy;

    @Override
    public void onCreateStamp(java.time.LocalDateTime now, Integer actorUserId) {
        if (createdBy == null) createdBy = actorUserId;
    }

    @Override
    public void onUpdateStamp(java.time.LocalDateTime now, Integer actorUserId) {
        updatedAt = now;
        updatedBy = actorUserId;
    }
}
