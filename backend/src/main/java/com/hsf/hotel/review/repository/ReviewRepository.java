package com.hsf.hotel.review.repository;

import com.hsf.hotel.booking.model.Booking;
import com.hsf.hotel.review.model.Review;
import com.hsf.hotel.room.model.Room;
import com.hsf.hotel.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Integer> {

    // Find reviews by room, ordered by newest first
    List<Review> findByRoomOrderByCreatedAtDesc(Room room);

    // Find reviews by user
    List<Review> findByUserOrderByCreatedAtDesc(User user);

    // Find review by booking (one booking = one review)
    Optional<Review> findByBooking(Booking booking);

    // Check if user already reviewed a booking
    boolean existsByBooking(Booking booking);

    // Check if user already reviewed a room
    boolean existsByUserAndRoom(User user, Room room);

    // Get average rating for a room
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.room = :room")
    Double getAverageRatingByRoom(@Param("room") Room room);

    // Count reviews for a room
    long countByRoom(Room room);

    // Get all reviews with pagination
    @Query("SELECT r FROM Review r ORDER BY r.createdAt DESC")
    List<Review> findAllOrderByCreatedAtDesc();
}
