package com.hsf.hotel.notification.api;

import com.hsf.hotel.config.ApiResponse;
import com.hsf.hotel.exception.ApiException;
import com.hsf.hotel.notification.model.Notification;
import com.hsf.hotel.user.model.User;
import com.hsf.hotel.notification.service.NotificationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@com.hsf.hotel.config.ApiController
@RequestMapping(com.hsf.hotel.config.ApiPaths.V1 + "/notifications")
public class NotificationApi {

    private final NotificationService notificationService;

    public NotificationApi(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    private User requireUser(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, com.hsf.hotel.config.ErrorCodes.UNAUTHORIZED, "Vui lòng đăng nhập");
        }
        return user;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<?>> getMyNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpSession session) {
        User user = requireUser(session);
        Page<Notification> notifs = notificationService.getUserNotifications(user.getId(), page, size);
        long unreadCount = notificationService.getUnreadCount(user.getId());
        
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "notifications", notifs.getContent(),
                "totalPages", notifs.getTotalPages(),
                "totalElements", notifs.getTotalElements(),
                "unreadCount", unreadCount
        )));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<ApiResponse<?>> markAsRead(@PathVariable Integer id, HttpSession session) {
        User user = requireUser(session);
        Notification notification = notificationService.getNotificationById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Notification not found"));
        
        if (!notification.getUser().getId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, com.hsf.hotel.config.ErrorCodes.FORBIDDEN, "Access denied");
        }
        
        notification.setIsRead(true);
        notificationService.save(notification);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Đã đánh dấu đã đọc")));
    }

    @PostMapping("/read-all")
    public ResponseEntity<ApiResponse<?>> markAllAsRead(HttpSession session) {
        User user = requireUser(session);
        notificationService.markAllAsRead(user.getId());
        return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Đã đánh dấu tất cả đã đọc")));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> deleteNotification(@PathVariable Integer id, HttpSession session) {
        User user = requireUser(session);
        Notification notification = notificationService.getNotificationById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Notification not found"));
        
        if (!notification.getUser().getId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, com.hsf.hotel.config.ErrorCodes.FORBIDDEN, "Access denied");
        }
        
        notificationService.deleteNotification(id);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Đã xóa thông báo")));
    }
}
