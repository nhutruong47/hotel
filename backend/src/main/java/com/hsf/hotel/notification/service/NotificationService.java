package com.hsf.hotel.notification.service;

import com.hsf.hotel.notification.model.Notification;
import com.hsf.hotel.notification.dto.NotificationResponse;
import com.hsf.hotel.user.model.User;
import com.hsf.hotel.notification.repository.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public Notification createNotification(User user, String title, String message, String type, String link) {
        Notification notification = new Notification(user, title, message, type);
        notification.setLink(link);
        return notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUserNotifications(Integer userId, int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page must be zero or greater");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100");
        }
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId,
                        PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(NotificationResponse::from);
    }

    public long getUnreadCount(Integer userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }

    public void markAllAsRead(Integer userId) {
        notificationRepository.markAllAsRead(userId);
    }

    @Transactional
    public boolean markAsRead(Integer id, Integer userId) {
        return notificationRepository.markAsReadByOwner(id, userId) > 0;
    }

    @Transactional
    public boolean deleteNotification(Integer id, Integer userId) {
        return notificationRepository.deleteByIdAndOwner(id, userId) > 0;
    }
}
