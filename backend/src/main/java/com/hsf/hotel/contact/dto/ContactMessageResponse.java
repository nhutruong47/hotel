package com.hsf.hotel.contact.dto;

import com.hsf.hotel.contact.model.ContactMessage;

import java.time.LocalDateTime;

/** Back-office view of a contact message (P0-04). Omits IP address and user agent. */
public record ContactMessageResponse(
        Integer id,
        String name,
        String email,
        String phone,
        String subject,
        String message,
        String type,
        String priority,
        String status,
        String adminNotes,
        Integer assignedToId,
        String assignedToUsername,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime respondedAt
) {
    public static ContactMessageResponse from(ContactMessage c) {
        if (c == null) return null;
        return new ContactMessageResponse(
                c.getId(),
                c.getName(),
                c.getEmail(),
                c.getPhone(),
                c.getSubject(),
                c.getMessage(),
                c.getType() != null ? c.getType().name() : null,
                c.getPriority() != null ? c.getPriority().name() : null,
                c.getStatus() != null ? c.getStatus().name() : null,
                c.getAdminNotes(),
                c.getAssignedTo() != null ? c.getAssignedTo().getId() : null,
                c.getAssignedTo() != null ? c.getAssignedTo().getUsername() : null,
                c.getCreatedAt(),
                c.getUpdatedAt(),
                c.getRespondedAt()
        );
    }
}
