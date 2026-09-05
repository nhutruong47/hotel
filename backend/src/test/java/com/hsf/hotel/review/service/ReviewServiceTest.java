package com.hsf.hotel.review.service;
import com.hsf.hotel.user.model.User;
import com.hsf.hotel.room.model.RoomTypeEntity;
import com.hsf.hotel.room.model.Room;
import com.hsf.hotel.review.service.ReviewService;
import com.hsf.hotel.review.model.Review;
import com.hsf.hotel.booking.model.BookingStatus;
import com.hsf.hotel.booking.model.Booking;

import com.hsf.hotel.exception.BusinessRuleException;
import com.hsf.hotel.exception.ForbiddenException;
import com.hsf.hotel.exception.ResourceNotFoundException;

import com.hsf.hotel.booking.repository.BookingRepository;
import com.hsf.hotel.review.repository.ReviewRepository;
import com.hsf.hotel.room.repository.RoomRepository;
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

    @Mock private ReviewRepository reviewRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private RoomRepository roomRepository;

    @InjectMocks private ReviewService reviewService;

    private User testUser;
    private Room testRoom;
    private Booking testBooking;
    private Review testReview;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1);
        testUser.setUsername("testuser");
        testUser.setFullName("Test User");
        testUser.setRole("USER");

        testRoom = new Room();
        testRoom.setId(1);
        testRoom.setRoomNumber("101");

        RoomTypeEntity testType = new RoomTypeEntity("DELUXE", "Phòng Deluxe");
        testType.setId(1);
        testRoom.setRoomType(testType);
        testRoom.setPricePerNight(new BigDecimal("500000"));

        testBooking = new Booking();
        testBooking.setId(1);
        testBooking.setUser(testUser);
        testBooking.setRoom(testRoom);
        testBooking.setCheckInDate(LocalDate.now().minusDays(3));
        testBooking.setCheckOutDate(LocalDate.now().minusDays(1));
        testBooking.setStatus(BookingStatus.CHECKED_OUT);
        testBooking.setTotalPrice(new BigDecimal("1000000"));

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
        when(bookingRepository.findById(1)).thenReturn(Optional.of(testBooking));
        when(reviewRepository.existsByBooking(testBooking)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenReturn(testReview);

        Review result = reviewService.createReview(testUser, 1, 5, "Excellent room!", 5, 5, 5, 5, 5);

        assertNotNull(result);
        assertEquals(5, result.getRating());
        assertEquals("Excellent room!", result.getComment());
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    void createReview_BookingNotFound() {
        when(bookingRepository.findById(99)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> {
            reviewService.createReview(testUser, 99, 5, "Good", 5, 5, 5, 5, 5);
        });
        assertTrue(ex.getMessage().contains("99"));
        assertTrue(ex.getMessage().contains("Booking"));
    }

    @Test
    void createReview_NotOwner() {
        User anotherUser = new User();
        anotherUser.setId(2);
        anotherUser.setUsername("another");

        when(bookingRepository.findById(1)).thenReturn(Optional.of(testBooking));

        ForbiddenException ex = assertThrows(ForbiddenException.class, () -> {
            reviewService.createReview(anotherUser, 1, 5, "Good", 5, 5, 5, 5, 5);
        });
        assertTrue(ex.getMessage().contains("quyền đánh giá"));
    }

    @Test
    void createReview_InvalidBookingStatus() {
        // Set status to PENDING_PAYMENT so the post-stay review rule triggers.
        testBooking.setStatus(BookingStatus.PENDING_PAYMENT);
        when(bookingRepository.findById(1)).thenReturn(Optional.of(testBooking));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> {
            reviewService.createReview(testUser, 1, 5, "Good", 5, 5, 5, 5, 5);
        });
        assertEquals("REVIEW_NOT_ALLOWED", ex.getCode());
    }

    @Test
    void createReview_PaidBookingNotEligible() {
        testBooking.setStatus(BookingStatus.PAID);
        when(bookingRepository.findById(1)).thenReturn(Optional.of(testBooking));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> {
            reviewService.createReview(testUser, 1, 5, "Good", 5, 5, 5, 5, 5);
        });
        assertEquals("REVIEW_NOT_ALLOWED", ex.getCode());
    }

    @Test
    void createReview_AlreadyReviewed() {
        when(bookingRepository.findById(1)).thenReturn(Optional.of(testBooking));
        when(reviewRepository.existsByBooking(testBooking)).thenReturn(true);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> {
            reviewService.createReview(testUser, 1, 5, "Good", 5, 5, 5, 5, 5);
        });
        assertEquals("ALREADY_REVIEWED", ex.getCode());
    }

    @Test
    void updateReview_Success() {
        when(reviewRepository.findById(1)).thenReturn(Optional.of(testReview));
        when(reviewRepository.save(any(Review.class))).thenReturn(testReview);

        Review result = reviewService.updateReview(testUser, 1, 4, "Updated comment", 4, 4, 4, 4, 4);

        assertEquals(4, result.getRating());
        assertEquals("Updated comment", result.getComment());
        assertNotNull(result.getUpdatedAt());
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    void updateReview_NotOwner() {
        User anotherUser = new User();
        anotherUser.setId(2);

        when(reviewRepository.findById(1)).thenReturn(Optional.of(testReview));

        ForbiddenException ex = assertThrows(ForbiddenException.class, () -> {
            reviewService.updateReview(anotherUser, 1, 4, "Updated", 4, 4, 4, 4, 4);
        });
        assertTrue(ex.getMessage().contains("quyền sửa"));
    }

    @Test
    void deleteReview_SuccessAsOwner() {
        when(reviewRepository.findById(1)).thenReturn(Optional.of(testReview));
        doNothing().when(reviewRepository).delete(testReview);

        reviewService.deleteReview(testUser, 1);

        verify(reviewRepository).delete(testReview);
    }

    @Test
    void deleteReview_SuccessAsAdmin() {
        User adminUser = new User();
        adminUser.setId(99);
        adminUser.setRole("ADMIN");

        when(reviewRepository.findById(1)).thenReturn(Optional.of(testReview));
        doNothing().when(reviewRepository).delete(testReview);

        reviewService.deleteReview(adminUser, 1);

        verify(reviewRepository).delete(testReview);
    }

    @Test
    void getReviewsByRoomId() {
        List<Review> reviews = Arrays.asList(testReview);
        when(roomRepository.findById(1)).thenReturn(Optional.of(testRoom));
        when(reviewRepository.findByRoomOrderByCreatedAtDesc(testRoom)).thenReturn(reviews);

        List<Review> result = reviewService.getReviewsByRoomId(1);

        assertEquals(1, result.size());
        assertEquals(testReview, result.get(0));
    }

    @Test
    void getAverageRating() {
        when(reviewRepository.getAverageRatingByRoom(testRoom)).thenReturn(4.5);

        Double result = reviewService.getAverageRating(testRoom);

        assertEquals(4.5, result);
    }

    @Test
    void getAverageRating_NoReviews() {
        when(reviewRepository.getAverageRatingByRoom(testRoom)).thenReturn(null);

        Double result = reviewService.getAverageRating(testRoom);

        assertEquals(0.0, result);
    }

    @Test
    void hasReview() {
        when(reviewRepository.existsByBooking(testBooking)).thenReturn(true);

        boolean result = reviewService.hasReview(testBooking);

        assertTrue(result);
    }
}
