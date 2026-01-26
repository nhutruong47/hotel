package com.hsf.hotel.service;

import com.hsf.hotel.model.*;
import com.hsf.hotel.repository.BookingRepository;
import com.hsf.hotel.repository.ReviewRepository;
import com.hsf.hotel.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private RoomRepository roomRepository;

    /**
     * Create a new review for a completed booking
     */
    @Transactional
    public Review createReview(User user, Integer bookingId, Integer rating, String comment) {
        // Validate booking exists
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt phòng"));

        // Check ownership
        if (!booking.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Bạn không có quyền đánh giá đơn này");
        }

        // Check booking status (must be CONFIRMED or COMPLETED)
        if (booking.getStatus() != BookingStatus.CONFIRMED &&
                booking.getStatus() != BookingStatus.COMPLETED) {
            throw new RuntimeException("Chỉ có thể đánh giá đơn đã xác nhận hoặc hoàn thành");
        }

        // Check if already reviewed
        if (reviewRepository.existsByBooking(booking)) {
            throw new RuntimeException("Bạn đã đánh giá đơn này rồi");
        }

        // Create review
        Review review = new Review();
        review.setUser(user);
        review.setRoom(booking.getRoom());
        review.setBooking(booking);
        review.setRating(rating);
        review.setComment(comment);
        review.setCreatedAt(LocalDateTime.now());

        return reviewRepository.save(review);
    }

    /**
     * Update an existing review
     */
    @Transactional
    public Review updateReview(User user, Integer reviewId, Integer rating, String comment) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đánh giá"));

        // Check ownership
        if (!review.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Bạn không có quyền sửa đánh giá này");
        }

        review.setRating(rating);
        review.setComment(comment);
        review.setUpdatedAt(LocalDateTime.now());

        return reviewRepository.save(review);
    }

    /**
     * Delete a review
     */
    @Transactional
    public void deleteReview(User user, Integer reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đánh giá"));

        // Check ownership (or admin can delete)
        if (!review.getUser().getId().equals(user.getId()) && !"ADMIN".equals(user.getRole())) {
            throw new RuntimeException("Bạn không có quyền xóa đánh giá này");
        }

        reviewRepository.delete(review);
    }

    /**
     * Get all reviews for a room
     */
    public List<Review> getReviewsByRoom(Room room) {
        return reviewRepository.findByRoomOrderByCreatedAtDesc(room);
    }

    /**
     * Get all reviews for a room by room ID
     */
    public List<Review> getReviewsByRoomId(Integer roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng"));
        return reviewRepository.findByRoomOrderByCreatedAtDesc(room);
    }

    /**
     * Get all reviews by a user
     */
    public List<Review> getReviewsByUser(User user) {
        return reviewRepository.findByUserOrderByCreatedAtDesc(user);
    }

    /**
     * Get review by ID
     */
    public Optional<Review> getReviewById(Integer id) {
        return reviewRepository.findById(id);
    }

    /**
     * Get review by booking
     */
    public Optional<Review> getReviewByBooking(Booking booking) {
        return reviewRepository.findByBooking(booking);
    }

    /**
     * Check if a booking has been reviewed
     */
    public boolean hasReview(Booking booking) {
        return reviewRepository.existsByBooking(booking);
    }

    /**
     * Get average rating for a room
     */
    public Double getAverageRating(Room room) {
        Double avg = reviewRepository.getAverageRatingByRoom(room);
        return avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0;
    }

    /**
     * Get review count for a room
     */
    public long getReviewCount(Room room) {
        return reviewRepository.countByRoom(room);
    }

    /**
     * Get all reviews (for admin)
     */
    public List<Review> getAllReviews() {
        return reviewRepository.findAllOrderByCreatedAtDesc();
    }
}
