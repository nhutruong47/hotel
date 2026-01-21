package com.hsf.hotel.repository;

import com.hsf.hotel.model.Booking;
import com.hsf.hotel.model.Review;
import com.hsf.hotel.model.Room;
import com.hsf.hotel.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Integer> {
    
    List<Review> findByRoomOrderByCreatedAtDesc(Room room);
    
    List<Review> findByUserOrderByCreatedAtDesc(User user);
    
    List<Review> findByRoomAndAnonymousOrderByCreatedAtDesc(Room room, boolean anonymous);
    
    // Check if user has already reviewed this room (through a specific booking)
    Review findByUserAndRoomAndBooking(User user, Room room, Booking booking);
    
    // Check if user has already reviewed this specific booking
    Review findByUserAndBooking(User user, Booking booking);
    
    // Get all reviews for a room excluding anonymous ones
    @Query("SELECT r FROM Review r WHERE r.room = :room AND r.anonymous = false ORDER BY r.createdAt DESC")
    List<Review> findPublicReviewsByRoom(@Param("room") Room room);
    
    // Get average rating for a room
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.room = :room")
    Double getAverageRatingForRoom(@Param("room") Room room);
    
    // Count reviews for a room
    @Query("SELECT COUNT(r) FROM Review r WHERE r.room = :room")
    Long countReviewsForRoom(@Param("room") Room room);
    
    // Count reviews for a room excluding anonymous ones
    @Query("SELECT COUNT(r) FROM Review r WHERE r.room = :room AND r.anonymous = false")
    Long countPublicReviewsForRoom(@Param("room") Room room);
}