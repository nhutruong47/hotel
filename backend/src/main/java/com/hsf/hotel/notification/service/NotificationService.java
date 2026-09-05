package com.hsf.hotel.notification.service;

import com.hsf.hotel.notification.model.Notification;
import com.hsf.hotel.user.model.User;
import com.hsf.hotel.notification.repository.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Optional;

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

    public Page<Notification> getUserNotifications(Integer userId, int page, int size) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId,
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
    }

    public long getUnreadCount(Integer userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }

    public void markAllAsRead(Integer userId) {
        notificationRepository.markAllAsRead(userId);
    }

    public Optional<Notification> getNotificationById(Integer id) {
        return notificationRepository.findById(id);
    }

    public Notification save(Notification notification) {
        return notificationRepository.save(notification);
    }

    public void deleteNotification(Integer id) {
        notificationRepository.deleteById(id);
    }
}
