package com.hsf.hotel.api;

import com.hsf.hotel.config.ApiResponse;
import com.hsf.hotel.config.ErrorCodes;
import com.hsf.hotel.exception.ApiException;
import com.hsf.hotel.model.User;
import com.hsf.hotel.service.BookingService;
import com.hsf.hotel.service.NotificationService;
import com.hsf.hotel.service.WishlistService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Customer-facing dashboard aggregator. Pulls the user's bookings, wishlist
 * size, and unread notification count into a single payload so the SPA does
 * not have to fire three parallel requests just to render the home shell.
 */
@RestController
@RequestMapping("/api/v1/account")
public class AccountApi {

    private final BookingService bookingService;
    private final WishlistService wishlistService;
    private final NotificationService notificationService;

    public AccountApi(BookingService bookingService,
                      WishlistService wishlistService,
                      NotificationService notificationService) {
        this.bookingService = bookingService;
        this.wishlistService = wishlistService;
        this.notificationService = notificationService;
    }

    private static User requireUser(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, ErrorCodes.UNAUTHORIZED, "Vui lòng đăng nhập");
        }
        return user;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse> dashboard(HttpSession session) {
        User user = requireUser(session);
        int wishlistCount = wishlistService.getUserWishlist(user.getId()).size();
        long unreadNotifications = notificationService.getUnreadCount(user.getId());
        return ResponseEntity.ok(ApiResponse.ok(
                bookingService.getCustomerDashboard(user, wishlistCount, unreadNotifications)));
    }
}
