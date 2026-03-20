package com.hsf.hotel.model;

import org.hibernate.annotations.Nationalized;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ChatMessages")
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Nationalized
    @Column(nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String userMessage;

    @Nationalized
    @Column(nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String aiResponse;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public ChatMessage() {
    }

    public ChatMessage(User user, String userMessage, String aiResponse) {
        this.user = user;
        this.userMessage = userMessage;
        this.aiResponse = aiResponse;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public void setUserMessage(String userMessage) {
        this.userMessage = userMessage;
    }

    public String getAiResponse() {
        return aiResponse;
    }

    public void setAiResponse(String aiResponse) {
        this.aiResponse = aiResponse;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
