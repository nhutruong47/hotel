package com.hsf.hotel;

import com.hsf.hotel.model.Booking;
import com.hsf.hotel.model.BookingStatus;
import com.hsf.hotel.model.Review;
import com.hsf.hotel.model.Room;
import com.hsf.hotel.model.RoomType;
import com.hsf.hotel.model.User;
import com.hsf.hotel.repository.BookingRepository;
import com.hsf.hotel.repository.ReviewRepository;
import com.hsf.hotel.service.ReviewService;
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
public class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;
    
    @Mock
    private BookingRepository bookingRepository;
    
    @InjectMocks
    private ReviewService reviewService;
    
    private User testUser;
    private Room testRoom;
    private Booking testBooking;
    private Review testReview;
    
    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User();
        testUser.setId(1);
        testUser.setUsername("testuser");
        testUser.setFullName("Test User");
        
        // Create test room
        testRoom = new Room();
        testRoom.setId(1);
        testRoom.setRoomNumber("101");
        testRoom.setRoomType(RoomType.STANDARD);
        testRoom.setPricePerNight(new BigDecimal("500000"));
        
        // Create test booking
        testBooking = new Booking();
        testBooking.setId(1);
        testBooking.setUser(testUser);
        testBooking.setRoom(testRoom);
        testBooking.setCheckInDate(LocalDate.now().minusDays(5));
        testBooking.setCheckOutDate(LocalDate.now().minusDays(3));
        testBooking.setStatus(BookingStatus.COMPLETED);
        
        // Create test review
        testReview = new Review();
        testReview.setId(1);
        testReview.setUser(testUser);
        testReview.setRoom(testRoom);
        testReview.setBooking(testBooking);
        testReview.setRating(5);
        testReview.setComment("Great room!");
        testReview.setUserName("Test User");
        testReview.setAnonymous(false);
        testReview.setCreatedAt(LocalDateTime.now());
    }
    
    @Test
    void testGetReviewsByRoom() {
        // Arrange
        List<Review> expectedReviews = Arrays.asList(testReview);
        when(reviewRepository.findByRoomOrderByCreatedAtDesc(testRoom)).thenReturn(expectedReviews);
        
        // Act
        List<Review> result = reviewService.getReviewsByRoom(testRoom);
        
        // Assert
        assertEquals(expectedReviews, result);
        verify(reviewRepository, times(1)).findByRoomOrderByCreatedAtDesc(testRoom);
    }
    
    @Test
    void testGetReviewsByUser() {
        // Arrange
        List<Review> expectedReviews = Arrays.asList(testReview);
        when(reviewRepository.findByUserOrderByCreatedAtDesc(testUser)).thenReturn(expectedReviews);
        
        // Act
        List<Review> result = reviewService.getReviewsByUser(testUser);
        
        // Assert
        assertEquals(expectedReviews, result);
        verify(reviewRepository, times(1)).findByUserOrderByCreatedAtDesc(testUser);
    }
    
    @Test
    void testGetPublicReviewsByRoom() {
        // Arrange
        List<Review> expectedReviews = Arrays.asList(testReview);
        when(reviewRepository.findPublicReviewsByRoom(testRoom)).thenReturn(expectedReviews);
        
        // Act
        List<Review> result = reviewService.getPublicReviewsByRoom(testRoom);
        
        // Assert
        assertEquals(expectedReviews, result);
        verify(reviewRepository, times(1)).findPublicReviewsByRoom(testRoom);
    }
    
    @Test
    void testHasUserReviewedBooking_ReturnsTrueWhenReviewExists() {
        // Arrange
        when(reviewRepository.findByUserAndBooking(testUser, testBooking)).thenReturn(testReview);
        
        // Act
        boolean result = reviewService.hasUserReviewedBooking(testUser, testBooking);
        
        // Assert
        assertTrue(result);
        verify(reviewRepository, times(1)).findByUserAndBooking(testUser, testBooking);
    }
    
    @Test
    void testHasUserReviewedBooking_ReturnsFalseWhenNoReviewExists() {
        // Arrange
        when(reviewRepository.findByUserAndBooking(testUser, testBooking)).thenReturn(null);
        
        // Act
        boolean result = reviewService.hasUserReviewedBooking(testUser, testBooking);
        
        // Assert
        assertFalse(result);
        verify(reviewRepository, times(1)).findByUserAndBooking(testUser, testBooking);
    }
    
    @Test
    void testCreateReviewSuccess() {
        // Arrange
        when(reviewRepository.findByUserAndBooking(testUser, testBooking)).thenReturn(null);
        when(reviewRepository.save(any(Review.class))).thenReturn(testReview);
        
        // Act
        Review result = reviewService.createReview(
                testUser, testRoom, testBooking, 5, "Great room!", false);
        
        // Assert
        assertNotNull(result);
        assertEquals(testRoom, result.getRoom());
        assertEquals(testUser, result.getUser());
        assertEquals(testBooking, result.getBooking());
        assertEquals(5, result.getRating());
        assertEquals("Great room!", result.getComment());
        assertFalse(result.isAnonymous());
        verify(reviewRepository, times(1)).save(any(Review.class));
    }
    
    @Test
    void testCreateReviewThrowsExceptionForInvalidRating() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            reviewService.createReview(testUser, testRoom, testBooking, 6, "Great room!", false);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            reviewService.createReview(testUser, testRoom, testBooking, 0, "Great room!", false);
        });
    }
    
    @Test
    void testCreateReviewThrowsExceptionWhenAlreadyReviewed() {
        // Arrange
        when(reviewRepository.findByUserAndBooking(testUser, testBooking)).thenReturn(testReview);
        
        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            reviewService.createReview(testUser, testRoom, testBooking, 5, "Great room!", false);
        });
    }
    
    @Test
    void testUpdateReviewSuccess() {
        // Arrange
        testReview.setRating(4); // Change rating from 5 to 4
        when(reviewRepository.findById(1)).thenReturn(Optional.of(testReview));
        when(reviewRepository.save(any(Review.class))).thenReturn(testReview);
        
        // Act
        Review result = reviewService.updateReview(1, testUser, 4, "Good room!", true);
        
        // Assert
        assertNotNull(result);
        assertEquals(4, result.getRating());
        assertEquals("Good room!", result.getComment());
        assertTrue(result.isAnonymous());
        verify(reviewRepository, times(1)).save(any(Review.class));
    }
    
    @Test
    void testUpdateReviewThrowsExceptionForInvalidRating() {
        // Arrange
        when(reviewRepository.findById(1)).thenReturn(Optional.of(testReview));
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            reviewService.updateReview(1, testUser, 6, "Great room!", false);
        });
    }
    
    @Test
    void testUpdateReviewThrowsExceptionWhenReviewNotFound() {
        // Arrange
        when(reviewRepository.findById(1)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            reviewService.updateReview(1, testUser, 4, "Good room!", false);
        });
    }
    
    @Test
    void testUpdateReviewThrowsExceptionWhenNotOwner() {
        // Arrange
        User anotherUser = new User();
        anotherUser.setId(2);
        anotherUser.setUsername("anotheruser");
        
        when(reviewRepository.findById(1)).thenReturn(Optional.of(testReview));
        
        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            reviewService.updateReview(1, anotherUser, 4, "Good room!", false);
        });
    }
    
    @Test
    void testDeleteReviewSuccess() {
        // Arrange
        when(reviewRepository.findById(1)).thenReturn(Optional.of(testReview));
        
        // Act
        reviewService.deleteReview(1, testUser);
        
        // Assert
        verify(reviewRepository, times(1)).delete(testReview);
    }
    
    @Test
    void testDeleteReviewThrowsExceptionWhenReviewNotFound() {
        // Arrange
        when(reviewRepository.findById(1)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            reviewService.deleteReview(1, testUser);
        });
    }
    
    @Test
    void testDeleteReviewThrowsExceptionWhenNotOwner() {
        // Arrange
        User anotherUser = new User();
        anotherUser.setId(2);
        anotherUser.setUsername("anotheruser");
        
        when(reviewRepository.findById(1)).thenReturn(Optional.of(testReview));
        
        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            reviewService.deleteReview(1, anotherUser);
        });
    }
    
    @Test
    void testGetAverageRatingForRoom() {
        // Arrange
        when(reviewRepository.getAverageRatingForRoom(testRoom)).thenReturn(4.5);
        
        // Act
        Double result = reviewService.getAverageRatingForRoom(testRoom);
        
        // Assert
        assertEquals(4.5, result);
        verify(reviewRepository, times(1)).getAverageRatingForRoom(testRoom);
    }
    
    @Test
    void testGetAverageRatingForRoomReturnsZeroWhenNoReviews() {
        // Arrange
        when(reviewRepository.getAverageRatingForRoom(testRoom)).thenReturn(null);
        
        // Act
        Double result = reviewService.getAverageRatingForRoom(testRoom);
        
        // Assert
        assertEquals(0.0, result);
        verify(reviewRepository, times(1)).getAverageRatingForRoom(testRoom);
    }
    
    @Test
    void testCountReviewsForRoom() {
        // Arrange
        when(reviewRepository.countReviewsForRoom(testRoom)).thenReturn(5L);
        
        // Act
        Long result = reviewService.countReviewsForRoom(testRoom);
        
        // Assert
        assertEquals(5L, result);
        verify(reviewRepository, times(1)).countReviewsForRoom(testRoom);
    }
    
    @Test
    void testIsBookingEligibleForReviewTrue() {
        // Act
        boolean result = reviewService.isBookingEligibleForReview(testBooking);
        
        // Assert
        assertTrue(result);
    }
    
    @Test
    void testIsBookingEligibleForReviewFalseForPendingBooking() {
        // Arrange
        testBooking.setStatus(BookingStatus.PENDING);
        
        // Act
        boolean result = reviewService.isBookingEligibleForReview(testBooking);
        
        // Assert
        assertFalse(result);
    }
    
    @Test
    void testIsBookingEligibleForReviewFalseForFutureBooking() {
        // Arrange
        testBooking.setCheckInDate(LocalDate.now().plusDays(5));
        testBooking.setCheckOutDate(LocalDate.now().plusDays(7));
        testBooking.setStatus(BookingStatus.COMPLETED);
        
        // Act
        boolean result = reviewService.isBookingEligibleForReview(testBooking);
        
        // Assert
        assertFalse(result);
    }
}