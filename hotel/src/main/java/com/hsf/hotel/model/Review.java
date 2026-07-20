package com.hsf.hotel.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "reviews", indexes = {
        @Index(name = "idx_reviews_room", columnList = "room_id"),
        @Index(name = "idx_reviews_user", columnList = "user_id"),
        @Index(name = "idx_reviews_rating", columnList = "rating"),
        @Index(name = "idx_reviews_booking", columnList = "booking_id"),
        @Index(name = "idx_reviews_created", columnList = "createdAt")
})
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    @JsonIgnore
    private Booking booking;

    @Column(nullable = false)
    private Integer rating; // 1-5 stars overall

    // Sub-category ratings (1-5 each, nullable = not rated)
    private Integer ratingCleanliness;
    private Integer ratingService;
    private Integer ratingLocation;
    private Integer ratingValue;
    private Integer ratingAmenities;

    @Column(length = 1000)
    private String comment;

    /** Uploaded review photos */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "review_photos", joinColumns = @JoinColumn(name = "review_id"))
    @Column(name = "photo_filename", length = 500)
    private List<String> photos = new ArrayList<>();

    /** Admin/management reply to the review */
    @Column(columnDefinition = "TEXT")
    private String adminReply;
    private LocalDateTime adminReplyAt;

    /** Content moderation */
    private Integer reportCount = 0;
    private Boolean isHidden = false;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;

    // Constructors
    public Review() {
    }

    public Review(User user, Room room, Booking booking, Integer rating, String comment) {
        this.user = user;
        this.room = room;
        this.booking = booking;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Helper method for displaying stars
    public String getStarsDisplay() {
        return "★".repeat(rating) + "☆".repeat(5 - rating);
    }

    // Sub-category ratings
    public Integer getRatingCleanliness() { return ratingCleanliness; }
    public void setRatingCleanliness(Integer ratingCleanliness) { this.ratingCleanliness = ratingCleanliness; }
    public Integer getRatingService() { return ratingService; }
    public void setRatingService(Integer ratingService) { this.ratingService = ratingService; }
    public Integer getRatingLocation() { return ratingLocation; }
    public void setRatingLocation(Integer ratingLocation) { this.ratingLocation = ratingLocation; }
    public Integer getRatingValue() { return ratingValue; }
    public void setRatingValue(Integer ratingValue) { this.ratingValue = ratingValue; }
    public Integer getRatingAmenities() { return ratingAmenities; }
    public void setRatingAmenities(Integer ratingAmenities) { this.ratingAmenities = ratingAmenities; }

    // Photos
    public List<String> getPhotos() { return photos; }
    public void setPhotos(List<String> photos) { this.photos = photos; }

    // Admin reply
    public String getAdminReply() { return adminReply; }
    public void setAdminReply(String adminReply) { this.adminReply = adminReply; }
    public LocalDateTime getAdminReplyAt() { return adminReplyAt; }
    public void setAdminReplyAt(LocalDateTime adminReplyAt) { this.adminReplyAt = adminReplyAt; }

    // Moderation
    public Integer getReportCount() { return reportCount; }
    public void setReportCount(Integer reportCount) { this.reportCount = reportCount; }
    public Boolean getIsHidden() { return isHidden; }
    public void setIsHidden(Boolean isHidden) { this.isHidden = isHidden; }
}
