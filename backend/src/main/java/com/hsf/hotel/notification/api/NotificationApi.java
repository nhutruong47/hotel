package com.hsf.hotel.notification.api;

import com.hsf.hotel.config.ApiResponse;
import com.hsf.hotel.config.ErrorCodes;
import com.hsf.hotel.exception.ApiException;
import com.hsf.hotel.notification.dto.NotificationResponse;
import com.hsf.hotel.notification.service.NotificationService;
import com.hsf.hotel.user.model.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@com.hsf.hotel.config.ApiController
@RequestMapping(com.hsf.hotel.config.ApiPaths.V1 + "/notifications")
public class NotificationApi {

    private final NotificationService notificationService;

    public NotificationApi(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<?>> getMyNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpSession session) {
        User user = requireUser(session);
        Page<NotificationResponse> notifications = notificationService.getUserNotifications(user.getId(), page, size);
        long unreadCount = notificationService.getUnreadCount(user.getId());

        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "notifications", notifications.getContent(),
                "totalPages", notifications.getTotalPages(),
                "totalElements", notifications.getTotalElements(),
                "unreadCount", unreadCount
        )));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<ApiResponse<?>> markAsRead(@PathVariable Integer id, HttpSession session) {
        User user = requireUser(session);
        if (!notificationService.markAsRead(id, user.getId())) {
            throw notFound();
        }
        return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Notification marked as read")));
    }

    @PostMapping("/read-all")
    public ResponseEntity<ApiResponse<?>> markAllAsRead(HttpSession session) {
        User user = requireUser(session);
        notificationService.markAllAsRead(user.getId());
        return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "All notifications marked as read")));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> deleteNotification(@PathVariable Integer id, HttpSession session) {
        User user = requireUser(session);
        if (!notificationService.deleteNotification(id, user.getId())) {
            throw notFound();
        }
        return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Notification deleted")));
    }

    private static User requireUser(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, ErrorCodes.UNAUTHORIZED, "Authentication required");
        }
        return user;
    }

    private static ApiException notFound() {
        // Use the same response for a missing row and another user's row so
        // notification IDs cannot be used as an account-enumeration oracle.
        return new ApiException(HttpStatus.NOT_FOUND, ErrorCodes.NOT_FOUND, "Notification not found");
    }
}
