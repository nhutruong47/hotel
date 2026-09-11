package com.hsf.hotel.notification.service;

import com.hsf.hotel.notification.model.Notification;
import com.hsf.hotel.notification.repository.NotificationRepository;
import com.hsf.hotel.user.model.User;
import com.hsf.hotel.user.model.UserRole;
import com.hsf.hotel.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class NotificationOwnershipIntegrationTest {

    @Autowired private NotificationService notificationService;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private UserRepository userRepository;

    @Test
    void ownerPredicateIsAppliedInsideUpdateAndDelete() {
        long suffix = System.nanoTime();
        User owner = createUser("notif_owner_" + suffix);
        User other = createUser("notif_other_" + suffix);

        Notification notification = notificationRepository.save(
                new Notification(owner, "Booking update", "Your booking changed", "BOOKING"));

        assertFalse(notificationService.markAsRead(notification.getId(), other.getId()));
        assertFalse(notificationRepository.findById(notification.getId()).orElseThrow().getIsRead());

        assertTrue(notificationService.markAsRead(notification.getId(), owner.getId()));
        assertTrue(notificationRepository.findById(notification.getId()).orElseThrow().getIsRead());

        assertFalse(notificationService.deleteNotification(notification.getId(), other.getId()));
        assertTrue(notificationRepository.existsById(notification.getId()));
        assertTrue(notificationService.deleteNotification(notification.getId(), owner.getId()));
        assertFalse(notificationRepository.existsById(notification.getId()));
    }

    @Test
    void notificationPageBoundsAreEnforced() {
        assertThrows(IllegalArgumentException.class,
                () -> notificationService.getUserNotifications(1, -1, 20));
        assertThrows(IllegalArgumentException.class,
                () -> notificationService.getUserNotifications(1, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> notificationService.getUserNotifications(1, 0, 101));
    }

    private User createUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@test.local");
        user.setPasswordHash("$2a$10$dummy");
        user.setRole(UserRole.USER);
        user.setEmailVerified(true);
        return userRepository.save(user);
    }
}
