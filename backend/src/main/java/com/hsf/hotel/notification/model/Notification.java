package com.hsf.hotel.notification.model;
import com.hsf.hotel.user.model.User;
import com.hsf.hotel.notification.model.NotificationType;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notif_user", columnList = "user_id"),
        @Index(name = "idx_notif_unread", columnList = "user_id, isRead")
})
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(nullable = false)
    private String type; // SYSTEM, BOOKING, PAYMENT, REFUND, PROMOTION — see NotificationType enum

    private Boolean isRead = false;

    private String link; // Optional deep link

    private LocalDateTime createdAt = LocalDateTime.now();

    public Notification() {}

    public Notification(User user, String title, String message, String type) {
        this.user = user;
        this.title = title;
        this.message = message;
        this.type = type;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Type-safe convenience constructor. Falls back to the supplied raw value
     * if the enum constant is null (e.g., legacy data).
     */
    public Notification(User user, String title, String message, NotificationType type) {
        this(user, title, message, type != null ? type.name() : null);
    }

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public void setType(NotificationType type) { this.type = type != null ? type.name() : null; }
    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean isRead) { this.isRead = isRead; }
    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
