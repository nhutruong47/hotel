package com.hsf.hotel.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "contact_messages", indexes = {
    @Index(name = "idx_contact_status", columnList = "status"),
    @Index(name = "idx_contact_created", columnList = "createdAt")
})
public class ContactMessage {

    public enum ContactStatus {
        PENDING("Chờ xử lý"),
        IN_PROGRESS("Đang xử lý"),
        RESOLVED("Đã giải quyết"),
        CLOSED("Đã đóng");

        private final String displayName;
        ContactStatus(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    public enum ContactPriority {
        LOW("Thấp"),
        NORMAL("Bình thường"),
        HIGH("Cao"),
        URGENT("Khẩn cấp");

        private final String displayName;
        ContactPriority(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    public enum ContactType {
        GENERAL("Chung"),
        RESERVATION("Đặt phòng"),
        SUPPORT("Hỗ trợ"),
        FEEDBACK("Phản hồi"),
        PARTNERSHIP("Hợp tác"),
        EMERGENCY("Khẩn cấp");

        private final String displayName;
        ContactType(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(length = 50)
    private String phone;

    @Column(nullable = false, length = 255)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContactType type = ContactType.GENERAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContactPriority priority = ContactPriority.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContactStatus status = ContactStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private User assignedTo;

    @Column(columnDefinition = "TEXT")
    private String adminNotes;

    private String ipAddress;

    private String userAgent;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;

    private LocalDateTime respondedAt;

    public ContactMessage() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public ContactType getType() { return type; }
    public void setType(ContactType type) { this.type = type; }
    public ContactPriority getPriority() { return priority; }
    public void setPriority(ContactPriority priority) { this.priority = priority; }
    public ContactStatus getStatus() { return status; }
    public void setStatus(ContactStatus status) { this.status = status; }
    public User getAssignedTo() { return assignedTo; }
    public void setAssignedTo(User assignedTo) { this.assignedTo = assignedTo; }
    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getRespondedAt() { return respondedAt; }
    public void setRespondedAt(LocalDateTime respondedAt) { this.respondedAt = respondedAt; }
}
