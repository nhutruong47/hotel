package com.hsf.hotel.service;

import com.hsf.hotel.exception.BusinessRuleException;
import com.hsf.hotel.exception.ForbiddenException;
import com.hsf.hotel.exception.ResourceNotFoundException;
import com.hsf.hotel.model.Booking;
import com.hsf.hotel.model.BookingStatus;
import com.hsf.hotel.model.Review;
import com.hsf.hotel.model.Room;
import com.hsf.hotel.model.User;
import com.hsf.hotel.repository.BookingRepository;
import com.hsf.hotel.repository.ReviewRepository;
import com.hsf.hotel.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;

    public ReviewService(ReviewRepository reviewRepository,
                         BookingRepository bookingRepository,
                         RoomRepository roomRepository) {
        this.reviewRepository = reviewRepository;
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
    }

    @Transactional
    public Review createReview(User user, Integer bookingId, Integer rating, String comment, Integer rClean, Integer rService, Integer rLoc, Integer rVal, Integer rAmen) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        if (!booking.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Bạn không có quyền đánh giá đơn này");
        }
        if (booking.getStatus() != BookingStatus.PAID
                && booking.getStatus() != BookingStatus.CHECKED_IN
                && booking.getStatus() != BookingStatus.CHECKED_OUT
                && booking.getStatus() != BookingStatus.COMPLETED) {
            throw new BusinessRuleException("REVIEW_NOT_ALLOWED",
                    "Chỉ có thể đánh giá đơn đã thanh toán, đã nhận phòng, đã trả phòng hoặc hoàn thành");
        }
        if (rating == null || rating < 1 || rating > 5) {
            throw new BusinessRuleException("INVALID_RATING", "Điểm đánh giá phải nằm trong khoảng 1-5");
        }
        if (reviewRepository.existsByBooking(booking)) {
            throw new BusinessRuleException("ALREADY_REVIEWED", "Bạn đã đánh giá đơn này rồi");
        }

        Review review = new Review();
        review.setUser(user);
        review.setRoom(booking.getRoom());
        review.setBooking(booking);
        review.setRating(rating);
        review.setComment(comment);
        review.setRatingCleanliness(rClean);
        review.setRatingService(rService);
        review.setRatingLocation(rLoc);
        review.setRatingValue(rVal);
        review.setRatingAmenities(rAmen);
        review.setCreatedAt(LocalDateTime.now());
        Review saved = reviewRepository.save(review);

        // Update room aggregate fields so list/featured pages don't have to recompute.
        Room room = booking.getRoom();
        long count = reviewRepository.countByRoom(room);
        Double avg = reviewRepository.getAverageRatingByRoom(room);
        room.setReviewCount(count);
        room.setAvgRating(avg != null
                ? new java.math.BigDecimal(String.valueOf(avg))
                : java.math.BigDecimal.ZERO);
        roomRepository.save(room);

        return saved;
    }

    @Transactional
    public Review updateReview(User user, Integer reviewId, Integer rating, String comment, Integer rClean, Integer rService, Integer rLoc, Integer rVal, Integer rAmen) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", reviewId));
        if (!review.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Bạn không có quyền sửa đánh giá này");
        }
        if (rating == null || rating < 1 || rating > 5) {
            throw new BusinessRuleException("INVALID_RATING", "Điểm đánh giá phải nằm trong khoảng 1-5");
        }
        review.setRating(rating);
        review.setComment(comment);
        review.setRatingCleanliness(rClean);
        review.setRatingService(rService);
        review.setRatingLocation(rLoc);
        review.setRatingValue(rVal);
        review.setRatingAmenities(rAmen);
        review.setUpdatedAt(LocalDateTime.now());
        Review saved = reviewRepository.save(review);

        Room room = review.getRoom();
        Double avg = reviewRepository.getAverageRatingByRoom(room);
        if (avg != null) {
            room.setAvgRating(new java.math.BigDecimal(String.valueOf(avg)));
        }
        roomRepository.save(room);
        return saved;
    }

    @Transactional
    public void deleteReview(User user, Integer reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", reviewId));
        if (!review.getUser().getId().equals(user.getId()) && !"ADMIN".equals(user.getRole())) {
            throw new ForbiddenException("Bạn không có quyền xóa đánh giá này");
        }
        Room room = review.getRoom();
        reviewRepository.delete(review);
        // Recompute aggregate fields.
        long count = reviewRepository.countByRoom(room);
        Double avg = reviewRepository.getAverageRatingByRoom(room);
        room.setReviewCount(count);
        room.setAvgRating(avg != null
                ? new java.math.BigDecimal(String.valueOf(avg))
                : java.math.BigDecimal.ZERO);
        roomRepository.save(room);
    }

    public List<Review> getReviewsByRoom(Room room) {
        return reviewRepository.findByRoomOrderByCreatedAtDesc(room);
    }

    public List<Review> getReviewsByRoomId(Integer roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", roomId));
        return reviewRepository.findByRoomOrderByCreatedAtDesc(room);
    }

    public List<Review> getReviewsByUser(User user) {
        return reviewRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public Optional<Review> getReviewById(Integer id) {
        return reviewRepository.findById(id);
    }

    public Optional<Review> getReviewByBooking(Booking booking) {
        return reviewRepository.findByBooking(booking);
    }

    public boolean hasReview(Booking booking) {
        return reviewRepository.existsByBooking(booking);
    }

    public Double getAverageRating(Room room) {
        Double avg = reviewRepository.getAverageRatingByRoom(room);
        return avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0;
    }

    public long getReviewCount(Room room) {
        return reviewRepository.countByRoom(room);
    }

    public List<Review> getAllReviews() {
        return reviewRepository.findAllOrderByCreatedAtDesc();
    }

    @Transactional
    public Review replyToReview(Integer reviewId, String reply) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", reviewId));
        review.setAdminReply(reply);
        review.setAdminReplyAt(LocalDateTime.now());
        return reviewRepository.save(review);
    }

    @Transactional
    public Review hideReview(Integer reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", reviewId));
        review.setIsHidden(true);
        return reviewRepository.save(review);
    }
}
