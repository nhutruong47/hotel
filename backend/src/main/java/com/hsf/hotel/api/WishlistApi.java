package com.hsf.hotel.api;

import com.hsf.hotel.config.ApiResponse;
import com.hsf.hotel.config.ErrorCodes;
import com.hsf.hotel.exception.ApiException;
import com.hsf.hotel.model.User;
import com.hsf.hotel.service.WishlistService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/wishlist")
public class WishlistApi {

    private final WishlistService wishlistService;

    public WishlistApi(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse> list(HttpSession session) {
        User user = requireUser(session);
        return ResponseEntity.ok(ApiResponse.ok(wishlistService.getUserWishlist(user.getId())));
    }

    @PostMapping("/toggle")
    public ResponseEntity<ApiResponse> toggle(@RequestBody Map<String, Integer> body, HttpSession session) {
        User user = requireUser(session);
        Integer roomId = body.get("roomId");
        if (roomId == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ErrorCodes.BAD_REQUEST, "Missing roomId"));
        }
        boolean isAdded = wishlistService.toggleWishlist(user.getId(), roomId);
        Map<String, Object> data = new HashMap<>();
        data.put("isAdded", isAdded);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    private static User requireUser(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, com.hsf.hotel.config.ErrorCodes.UNAUTHORIZED, "Vui lòng đăng nhập");
        }
        return user;
    }
}
