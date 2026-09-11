package com.hsf.hotel.notification.dto;

import com.hsf.hotel.notification.model.Notification;

import java.time.LocalDateTime;

/** Public notification shape without the owning User relationship. */
public record NotificationResponse(
        Integer id,
        String title,
        String message,
        String type,
        Boolean isRead,
        String link,
        LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getType(),
                notification.getIsRead(),
                notification.getLink(),
                notification.getCreatedAt()
        );
    }
}
