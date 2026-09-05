package com.hsf.hotel.user.dto;

import com.hsf.hotel.user.model.User;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Sanitised projection of {@link User} intended for serialisation in any
 * API response. Sensitive fields (password, tokens, identity, address) are
 * never exposed — they are mapped only into internal services or the audit
 * log.
 */
public final class UserSummaryDTO {

    private final Integer id;
    private final String username;
    private final String email;
    private final String fullName;
    private final String role;
    private final Boolean emailVerified;
    private final String avatarFilename;
    private final Boolean disabled;
    private final LocalDateTime createdAt;

    private UserSummaryDTO(Integer id, String username, String email, String fullName, String role,
                           Boolean emailVerified, String avatarFilename,
                           Boolean disabled, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.emailVerified = emailVerified;
        this.avatarFilename = avatarFilename;
        this.disabled = disabled;
        this.createdAt = createdAt;
    }

    public static UserSummaryDTO from(User u) {
        return new UserSummaryDTO(
                u.getId(),
                u.getUsername(),
                u.getEmail(),
                u.getFullName(),
                u.getRole(),
                u.getEmailVerified(),
                u.getAvatarFilename(),
                u.getDisabled() != null && u.getDisabled(),
                u.getCreatedAt()
        );
    }

    /** Convenience for callers that want a {@link Map} (e.g. Auth API). */
    public Map<String, Object> asMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("username", username);
        m.put("email", email);
        m.put("fullName", fullName);
        m.put("role", role);
        m.put("emailVerified", emailVerified);
        m.put("avatarFilename", avatarFilename);
        m.put("disabled", disabled);
        if (createdAt != null) {
            m.put("createdAt", createdAt.toString());
        }
        return m;
    }

    public Integer getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public String getRole() { return role; }
    public Boolean getEmailVerified() { return emailVerified; }
    public String getAvatarFilename() { return avatarFilename; }
    public Boolean getDisabled() { return disabled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}