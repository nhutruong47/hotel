package com.hsf.hotel.service;

import com.hsf.hotel.model.*;
import com.hsf.hotel.repository.BookingRepository;
import com.hsf.hotel.repository.ReviewRepository;
import com.hsf.hotel.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private RoomRepository roomRepository;

    @InjectMocks
    private ReviewService reviewService;

    private User testUser;
    private Room testRoom;
    private Booking testBooking;
    private Review testReview;

    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = new User();
        testUser.setId(1);
        testUser.setUsername("testuser");
        testUser.setFullName("Test User");
        testUser.setRole("USER");

        // Setup test room
        testRoom = new Room();
        testRoom.setId(1);
        testRoom.setRoomNumber("101");

        RoomTypeEntity testType = new RoomTypeEntity("DELUXE", "Phòng Deluxe");
        testType.setId(1);
        testRoom.setRoomType(testType);

        testRoom.setPricePerNight(new BigDecimal("500000"));

        // Setup test booking
        testBooking = new Booking();
        testBooking.setId(1);
        testBooking.setUser(testUser);
        testBooking.setRoom(testRoom);
        testBooking.setCheckInDate(LocalDate.now().minusDays(3));
        testBooking.setCheckOutDate(LocalDate.now().minusDays(1));
        testBooking.setStatus(BookingStatus.CONFIRMED);
        testBooking.setTotalPrice(new BigDecimal("1000000"));

        // Setup test review
        testReview = new Review();
        testReview.setId(1);
        testReview.setUser(testUser);
        testReview.setRoom(testRoom);
        testReview.setBooking(testBooking);
        testReview.setRating(5);
        testReview.setComment("Excellent room!");
        testReview.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void createReview_Success() {
        // Arrange
        when(bookingRepository.findById(1)).thenReturn(Optional.of(testBooking));
        when(reviewRepository.existsByBooking(testBooking)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenReturn(testReview);

        // Act
        Review result = reviewService.createReview(testUser, 1, 5, "Excellent room!");

        // Assert
        assertNotNull(result);
        assertEquals(5, result.getRating());
        assertEquals("Excellent room!", result.getComment());
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    void createReview_BookingNotFound() {
        // Arrange
        when(bookingRepository.findById(99)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            reviewService.createReview(testUser, 99, 5, "Good");
        });
        assertEquals("Booking not found", exception.getMessage());
    }

    @Test
    void createReview_NotOwner() {
        // Arrange
        User anotherUser = new User();
        anotherUser.setId(2);
        anotherUser.setUsername("another");

        when(bookingRepository.findById(1)).thenReturn(Optional.of(testBooking));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            reviewService.createReview(anotherUser, 1, 5, "Good");
        });
        assertEquals("You do not have permission to review this booking", exception.getMessage());
    }

    @Test
    void createReview_InvalidBookingStatus() {
        // Arrange
        testBooking.setStatus(BookingStatus.PENDING);
        when(bookingRepository.findById(1)).thenReturn(Optional.of(testBooking));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            reviewService.createReview(testUser, 1, 5, "Good");
        });
        assertEquals("Only confirmed or completed bookings can be reviewed", exception.getMessage());
    }

    @Test
    void createReview_AlreadyReviewed() {
        // Arrange
        when(bookingRepository.findById(1)).thenReturn(Optional.of(testBooking));
        when(reviewRepository.existsByBooking(testBooking)).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            reviewService.createReview(testUser, 1, 5, "Good");
        });
        assertEquals("You have already reviewed this booking", exception.getMessage());
    }

    @Test
    void updateReview_Success() {
        // Arrange
        when(reviewRepository.findById(1)).thenReturn(Optional.of(testReview));
        when(reviewRepository.save(any(Review.class))).thenReturn(testReview);

        // Act
        Review result = reviewService.updateReview(testUser, 1, 4, "Updated comment");

        // Assert
        assertEquals(4, result.getRating());
        assertEquals("Updated comment", result.getComment());
        assertNotNull(result.getUpdatedAt());
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    void updateReview_NotOwner() {
        // Arrange
        User anotherUser = new User();
        anotherUser.setId(2);

        when(reviewRepository.findById(1)).thenReturn(Optional.of(testReview));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            reviewService.updateReview(anotherUser, 1, 4, "Updated");
        });
        assertEquals("You do not have permission to edit this review", exception.getMessage());
    }

    @Test
    void deleteReview_SuccessAsOwner() {
        // Arrange
        when(reviewRepository.findById(1)).thenReturn(Optional.of(testReview));
        doNothing().when(reviewRepository).delete(testReview);

        // Act
        reviewService.deleteReview(testUser, 1);

        // Assert
        verify(reviewRepository).delete(testReview);
    }

    @Test
    void deleteReview_SuccessAsAdmin() {
        // Arrange
        User adminUser = new User();
        adminUser.setId(99);
        adminUser.setRole("ADMIN");

        when(reviewRepository.findById(1)).thenReturn(Optional.of(testReview));
        doNothing().when(reviewRepository).delete(testReview);

        // Act
        reviewService.deleteReview(adminUser, 1);

        // Assert
        verify(reviewRepository).delete(testReview);
    }

    @Test
    void getReviewsByRoomId() {
        // Arrange
        List<Review> reviews = Arrays.asList(testReview);
        when(roomRepository.findById(1)).thenReturn(Optional.of(testRoom));
        when(reviewRepository.findByRoomOrderByCreatedAtDesc(testRoom)).thenReturn(reviews);

        // Act
        List<Review> result = reviewService.getReviewsByRoomId(1);

        // Assert
        assertEquals(1, result.size());
        assertEquals(testReview, result.get(0));
    }

    @Test
    void getAverageRating() {
        // Arrange
        when(reviewRepository.getAverageRatingByRoom(testRoom)).thenReturn(4.5);

        // Act
        Double result = reviewService.getAverageRating(testRoom);

        // Assert
        assertEquals(4.5, result);
    }

    @Test
    void getAverageRating_NoReviews() {
        // Arrange
        when(reviewRepository.getAverageRatingByRoom(testRoom)).thenReturn(null);

        // Act
        Double result = reviewService.getAverageRating(testRoom);

        // Assert
        assertEquals(0.0, result);
    }

    @Test
    void hasReview() {
        // Arrange
        when(reviewRepository.existsByBooking(testBooking)).thenReturn(true);

        // Act
        boolean result = reviewService.hasReview(testBooking);

        // Assert
        assertTrue(result);
    }
}
