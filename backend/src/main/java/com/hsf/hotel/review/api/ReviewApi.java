package com.hsf.hotel.review.api;

import com.hsf.hotel.config.ApiResponse;
import com.hsf.hotel.config.ErrorCodes;
import com.hsf.hotel.exception.ApiException;
import com.hsf.hotel.exception.ForbiddenException;
import com.hsf.hotel.exception.ResourceNotFoundException;
import com.hsf.hotel.booking.model.Booking;
import com.hsf.hotel.review.model.Review;
import com.hsf.hotel.room.model.Room;
import com.hsf.hotel.user.model.User;
import com.hsf.hotel.booking.service.BookingService;
import com.hsf.hotel.review.service.ReviewService;
import com.hsf.hotel.room.service.RoomService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/reviews")
public class ReviewApi {

    private final ReviewService reviewService;
    private final BookingService bookingService;
    private final RoomService roomService;

    public ReviewApi(ReviewService reviewService,
                     BookingService bookingService,
                     RoomService roomService) {
        this.reviewService = reviewService;
        this.bookingService = bookingService;
        this.roomService = roomService;
    }

    public static class ReviewRequest {
        @NotNull(message = "Vui lòng chọn số sao")
        @Min(value = 1, message = "Đánh giá tối thiểu là 1 sao")
        @Max(value = 5, message = "Đánh giá tối đa là 5 sao")
        public Integer rating;

        public Integer ratingCleanliness;
        public Integer ratingService;
        public Integer ratingLocation;
        public Integer ratingValue;
        public Integer ratingAmenities;

        @Size(max = 1000, message = "Bình luận tối đa 1000 ký tự")
        public String comment;
    }

    @GetMapping("/room/{roomId}")
    public ResponseEntity<ApiResponse> forRoom(@PathVariable Integer roomId) {
        Room room = roomService.getRoomById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", roomId));
        List<Review> reviews = reviewService.getReviewsByRoomId(roomId);
        Map<String, Object> data = new HashMap<>();
        data.put("room", Map.of("id", room.getId(), "roomNumber", room.getRoomNumber()));
        data.put("reviews", reviews);
        data.put("avgRating", reviewService.getAverageRating(room));
        data.put("reviewCount", reviewService.getReviewCount(room));

        // Calculate category averages
        if (!reviews.isEmpty()) {
            double avgC = reviews.stream().filter(r -> r.getRatingCleanliness() != null).mapToInt(Review::getRatingCleanliness).average().orElse(0);
            double avgS = reviews.stream().filter(r -> r.getRatingService() != null).mapToInt(Review::getRatingService).average().orElse(0);
            double avgL = reviews.stream().filter(r -> r.getRatingLocation() != null).mapToInt(Review::getRatingLocation).average().orElse(0);
            double avgV = reviews.stream().filter(r -> r.getRatingValue() != null).mapToInt(Review::getRatingValue).average().orElse(0);
            double avgA = reviews.stream().filter(r -> r.getRatingAmenities() != null).mapToInt(Review::getRatingAmenities).average().orElse(0);
            data.put("categoryAverages", Map.of(
                "cleanliness", avgC,
                "service", avgS,
                "location", avgL,
                "value", avgV,
                "amenities", avgA
            ));
        }

        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/mine")
    public ResponseEntity<ApiResponse> mine(HttpSession session) {
        User user = requireUser(session);
        return ResponseEntity.ok(ApiResponse.ok(reviewService.getReviewsByUser(user)));
    }

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<ApiResponse> forBooking(@PathVariable Integer bookingId, HttpSession session) {
        User user = requireUser(session);
        Booking booking = bookingService.getBookingById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
        if (!booking.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Bạn không có quyền đánh giá đơn này");
        }
        Optional<Review> existing = reviewService.getReviewByBooking(booking);
        Map<String, Object> data = new HashMap<>();
        data.put("booking", booking);
        data.put("room", booking.getRoom());
        data.put("existing", existing.orElse(null));
        data.put("isEdit", existing.isPresent());
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @PostMapping("/booking/{bookingId}")
    public ResponseEntity<ApiResponse> submit(@PathVariable Integer bookingId,
                                              @Valid @RequestBody ReviewRequest body,
                                              HttpSession session) {
        User user = requireUser(session);
        Review r = reviewService.createReview(user, bookingId, body.rating, body.comment, body.ratingCleanliness, body.ratingService, body.ratingLocation, body.ratingValue, body.ratingAmenities);
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "review", r,
                "avgRating", reviewService.getAverageRating(r.getRoom()),
                "reviewCount", reviewService.getReviewCount(r.getRoom()),
                "message", "Cảm ơn bạn đã đánh giá!"
        )));
    }

    @PutMapping("/{reviewId}")
    public ResponseEntity<ApiResponse> update(@PathVariable Integer reviewId,
                                              @Valid @RequestBody ReviewRequest body,
                                              HttpSession session) {
        User user = requireUser(session);
        Review r = reviewService.updateReview(user, reviewId, body.rating, body.comment, body.ratingCleanliness, body.ratingService, body.ratingLocation, body.ratingValue, body.ratingAmenities);
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "review", r,
                "message", "Đã cập nhật đánh giá!"
        )));
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<ApiResponse> delete(@PathVariable Integer reviewId, HttpSession session) {
        User user = requireUser(session);
        reviewService.deleteReview(user, reviewId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Đã xóa đánh giá!")));
    }

    private static User requireUser(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, ErrorCodes.UNAUTHORIZED, "Chưa đăng nhập");
        }
        return user;
    }
}
