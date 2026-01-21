package com.hsf.hotel.service;

import com.hsf.hotel.model.Booking;
import com.hsf.hotel.model.Review;
import com.hsf.hotel.model.Room;
import com.hsf.hotel.model.User;
import com.hsf.hotel.repository.BookingRepository;
import com.hsf.hotel.repository.ReviewRepository;
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

    public List<Review> getReviewsByRoom(Room room) {
        return reviewRepository.findByRoomOrderByCreatedAtDesc(room);
    }

    public List<Review> getReviewsByUser(User user) {
        return reviewRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public List<Review> getPublicReviewsByRoom(Room room) {
        return reviewRepository.findPublicReviewsByRoom(room);
    }

    public Optional<Review> getReviewById(Integer id) {
        return reviewRepository.findById(id);
    }

    public boolean hasUserReviewedRoom(User user, Room room, Booking booking) {
        Review existingReview = reviewRepository.findByUserAndRoomAndBooking(user, room, booking);
        return existingReview != null;
    }

    public boolean hasUserReviewedBooking(User user, Booking booking) {
        Review existingReview = reviewRepository.findByUserAndBooking(user, booking);
        return existingReview != null;
    }

    @Transactional
    public Review createReview(User user, Room room, Booking booking, Integer rating, String comment, 
                             boolean anonymous) {
        // Validate rating
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        // Check if user has already reviewed this booking
        if (hasUserReviewedBooking(user, booking)) {
            throw new IllegalStateException("You have already reviewed this booking");
        }

        // Create new review
        Review review = new Review();
        review.setUser(user);
        review.setRoom(room);
        review.setBooking(booking);
        review.setRating(rating);
        review.setComment(comment);
        review.setUserName(user.getFullName() != null ? user.getFullName() : user.getUsername());
        review.setAnonymous(anonymous);
        review.setCreatedAt(LocalDateTime.now());

        return reviewRepository.save(review);
    }

    @Transactional
    public Review updateReview(Integer reviewId, User user, Integer rating, String comment, boolean anonymous) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found"));

        // Check if the user is the owner of the review
        if (!review.getUser().getId().equals(user.getId())) {
            throw new IllegalStateException("You can only edit your own reviews");
        }

        // Validate rating
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        // Update review
        review.setRating(rating);
        review.setComment(comment);
        review.setAnonymous(anonymous);

        return reviewRepository.save(review);
    }

    @Transactional
    public void deleteReview(Integer reviewId, User user) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found"));

        // Check if the user is the owner of the review
        if (!review.getUser().getId().equals(user.getId())) {
            throw new IllegalStateException("You can only delete your own reviews");
        }

        reviewRepository.delete(review);
    }

    public Double getAverageRatingForRoom(Room room) {
        Double avgRating = reviewRepository.getAverageRatingForRoom(room);
        return avgRating != null ? avgRating : 0.0;
    }

    public Long countReviewsForRoom(Room room) {
        return reviewRepository.countReviewsForRoom(room);
    }

    public Long countPublicReviewsForRoom(Room room) {
        return reviewRepository.countPublicReviewsForRoom(room);
    }

    // Check if a booking is eligible for review (booking must be completed)
    public boolean isBookingEligibleForReview(Booking booking) {
        return booking != null && 
               (booking.getStatus().toString().equals("COMPLETED") 
                || booking.getStatus().toString().equals("CONFIRMED")) &&
               booking.getCheckOutDate().isBefore(java.time.LocalDate.now());
    }

    // Get eligible bookings for a user that can be reviewed
    public List<Booking> getEligibleBookingsForReview(User user) {
        List<Booking> allBookings = new java.util.ArrayList<>();
        allBookings.addAll(bookingRepository.findByUserAndStatusOrderByCreatedAtDesc(user, 
               com.hsf.hotel.model.BookingStatus.COMPLETED));
        allBookings.addAll(bookingRepository.findByUserAndStatusOrderByCreatedAtDesc(user, 
               com.hsf.hotel.model.BookingStatus.CONFIRMED));
               
        return allBookings.stream()
               .filter(booking -> isBookingEligibleForReview(booking))
               .filter(booking -> !hasUserReviewedBooking(user, booking))
               .toList();
    }
}