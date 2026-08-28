package com.hsf.hotel.service;

import com.hsf.hotel.exception.BusinessRuleException;
import com.hsf.hotel.exception.ResourceNotFoundException;
import com.hsf.hotel.model.Booking;
import com.hsf.hotel.model.BookingStatus;
import com.hsf.hotel.model.Room;
import com.hsf.hotel.model.RoomTypeEntity;
import com.hsf.hotel.model.User;
import com.hsf.hotel.model.UserRole;
import com.hsf.hotel.repository.BookingRepository;
import com.hsf.hotel.repository.BookingStatusTransitionRepository;
import com.hsf.hotel.repository.RoomRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookingService Tests")
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingStatusTransitionRepository transitionRepository;

    @Mock
    private NotificationProducer notificationProducer;

    @Mock
    private VoucherService voucherService;

    @Mock
    private EntityManager entityManager;

    @Mock
    private NotificationService notificationService;

    private BookingService bookingService;

    private User testUser;
    private User adminUser;
    private Room testRoom;
    private Booking testBooking;

    @BeforeEach
    void setUp() {
        bookingService = new BookingService(
                bookingRepository,
                transitionRepository,
                notificationProducer,
                voucherService,
                entityManager,
                notificationService,
                24,
                "admin@hotel.com");

        // Setup test user
        testUser = new User();
        testUser.setId(1);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setRole("USER");

        // Setup admin user
        adminUser = new User();
        adminUser.setId(2);
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@example.com");
        adminUser.setRole(UserRole.ADMIN);

        // Setup test room
        testRoom = new Room();
        testRoom.setId(1);
        testRoom.setRoomNumber("GV-01");
        testRoom.setPricePerNight(BigDecimal.valueOf(450));
        testRoom.setCapacity(4);
        testRoom.setBedrooms(2);
        testRoom.setIsAvailable(true);

        // Setup test booking
        testBooking = new Booking();
        testBooking.setId(100);
        testBooking.setUser(testUser);
        testBooking.setRoom(testRoom);
        testBooking.setCheckInDate(LocalDate.now().plusDays(5));
        testBooking.setCheckOutDate(LocalDate.now().plusDays(7));
        testBooking.setGuestName("Test Guest");
        testBooking.setGuestEmail("guest@example.com");
        testBooking.setGuests(2);
        testBooking.setStatus(BookingStatus.PENDING_PAYMENT);
        testBooking.setTotalPrice(BigDecimal.valueOf(900));
        testBooking.setSubtotalPrice(BigDecimal.valueOf(900));
    }

    @Nested
    @DisplayName("Pricing Calculation Tests")
    class PricingTests {

        @Test
        @DisplayName("Should calculate total price correctly for multi-night stay")
        void testCalculatePricingMultiNight() {
            LocalDate checkIn = LocalDate.now().plusDays(1);
            LocalDate checkOut = LocalDate.now().plusDays(4); // 3 nights

            var pricing = bookingService.calculatePricing(testRoom, checkIn, checkOut);

            // 3 nights * 450 = 1350 subtotal
            assertEquals(BigDecimal.valueOf(1350), pricing.subtotal());
            // 8% service fee = 108
            assertEquals(BigDecimal.valueOf(108), pricing.serviceFee());
            // 10% tax = 135
            assertEquals(BigDecimal.valueOf(135), pricing.taxAmount());
            // Total = 1350 + 108 + 135 = 1593
            assertEquals(BigDecimal.valueOf(1593), pricing.total());
        }

        @Test
        @DisplayName("Should apply day-use price for same-day booking")
        void testCalculatePricingDayUse() {
            LocalDate sameDay = LocalDate.now().plusDays(1);

            var pricing = bookingService.calculatePricing(testRoom, sameDay, sameDay);

            // 0 nights = 50% of price per night = 225
            assertEquals(0, BigDecimal.valueOf(225).compareTo(pricing.subtotal()));
        }

        @Test
        @DisplayName("Should calculate correct pricing for expensive room")
        void testCalculatePricingExpensiveRoom() {
            Room expensiveRoom = new Room();
            expensiveRoom.setId(10);
            expensiveRoom.setRoomNumber("SE-04");
            expensiveRoom.setPricePerNight(BigDecimal.valueOf(2500));
            expensiveRoom.setCapacity(8);
            expensiveRoom.setIsAvailable(true);

            LocalDate checkIn = LocalDate.now().plusDays(1);
            LocalDate checkOut = LocalDate.now().plusDays(3); // 2 nights

            var pricing = bookingService.calculatePricing(expensiveRoom, checkIn, checkOut);

            // 2 nights * 2500 = 5000 subtotal
            assertEquals(BigDecimal.valueOf(5000), pricing.subtotal());
            // 8% service fee = 400
            assertEquals(BigDecimal.valueOf(400), pricing.serviceFee());
            // 10% tax = 500
            assertEquals(BigDecimal.valueOf(500), pricing.taxAmount());
            // Total = 5000 + 400 + 500 = 5900
            assertEquals(BigDecimal.valueOf(5900), pricing.total());
        }
    }

    @Nested
    @DisplayName("Availability Tests")
    class AvailabilityTests {

        @Test
        @DisplayName("Should return false when room is not available")
        void testIsRoomAvailableWhenUnavailable() {
            testRoom.setIsAvailable(false);

            boolean available = bookingService.isRoomAvailable(testRoom, 
                    LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));

            assertFalse(available);
        }

        @Test
        @DisplayName("Should return true when no conflicting bookings")
        void testIsRoomAvailableNoConflicts() {
            when(bookingRepository.findConflictingBookings(any(), any(), any()))
                    .thenReturn(List.of());

            boolean available = bookingService.isRoomAvailable(testRoom,
                    LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));

            assertTrue(available);
        }
    }

    @Nested
    @DisplayName("Booking Modification Tests")
    class ModificationTests {

        @Test
        @DisplayName("Should throw exception when user tries to modify other's booking")
        void testModifyBookingForbidden() {
            User otherUser = new User();
            otherUser.setId(999);
            otherUser.setUsername("other");
            otherUser.setRole(UserRole.USER);

            when(bookingRepository.findById(100)).thenReturn(Optional.of(testBooking));

            assertThrows(com.hsf.hotel.exception.ForbiddenException.class, () -> {
                bookingService.modifyBooking(100, otherUser, 
                        LocalDate.now().plusDays(6), LocalDate.now().plusDays(8), 3);
            });
        }

        @Test
        @DisplayName("Should throw exception when modifying cancelled booking")
        void testModifyCancelledBooking() {
            testBooking.setStatus(BookingStatus.CANCELLED);
            when(bookingRepository.findById(100)).thenReturn(Optional.of(testBooking));

            BusinessRuleException exception = assertThrows(BusinessRuleException.class, () -> {
                bookingService.modifyBooking(100, testUser,
                        LocalDate.now().plusDays(6), LocalDate.now().plusDays(8), 3);
            });

            assertEquals("INVALID_STATE", exception.getCode());
        }

        @Test
        @DisplayName("Should throw exception when guest count exceeds capacity")
        void testModifyBookingExceedsCapacity() {
            when(bookingRepository.findById(100)).thenReturn(Optional.of(testBooking));
            when(bookingRepository.findConflictingBookingsForUpdateExcluding(any(), anyInt(), any(), any()))
                    .thenReturn(List.of());

            BusinessRuleException exception = assertThrows(BusinessRuleException.class, () -> {
                bookingService.modifyBooking(100, testUser,
                        LocalDate.now().plusDays(6), LocalDate.now().plusDays(8), 10); // 10 guests > 4 capacity
            });

            assertTrue(exception.getMessage().contains("vượt quá sức chứa"));
        }
    }

    @Nested
    @DisplayName("Cancellation Tests")
    class CancellationTests {

        @Test
        @DisplayName("Should throw exception when cancelling already cancelled booking")
        void testCancelAlreadyCancelled() {
            testBooking.setStatus(BookingStatus.CANCELLED);
            when(bookingRepository.findById(100)).thenReturn(Optional.of(testBooking));

            BusinessRuleException exception = assertThrows(BusinessRuleException.class, () -> {
                bookingService.cancelBooking(100, testUser);
            });

            assertTrue(exception.getMessage().contains("đã được hủy"));
        }

        @Test
        @DisplayName("Should throw exception when cancelling completed booking")
        void testCancelCompletedBooking() {
            testBooking.setStatus(BookingStatus.COMPLETED);
            when(bookingRepository.findById(100)).thenReturn(Optional.of(testBooking));

            BusinessRuleException exception = assertThrows(BusinessRuleException.class, () -> {
                bookingService.cancelBooking(100, testUser);
            });

            assertTrue(exception.getMessage().contains("đã hoàn thành"));
        }

        @Test
        @DisplayName("Admin should be able to cancel any booking")
        void testAdminCancelAnyBooking() {
            User anotherUser = new User();
            anotherUser.setId(999);
            anotherUser.setUsername("another");
            anotherUser.setRole(UserRole.USER);
            testBooking.setUser(anotherUser);
            testBooking.setStatus(BookingStatus.PAID);
            testBooking.setTotalPrice(BigDecimal.valueOf(900));

            when(bookingRepository.findById(100)).thenReturn(Optional.of(testBooking));
            when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);

            Booking cancelled = bookingService.cancelBooking(100, adminUser);

            assertEquals(BookingStatus.CANCELLED, cancelled.getStatus());
        }
    }

    @Nested
    @DisplayName("Date Validation Tests")
    class DateValidationTests {

        @Test
        @DisplayName("Should throw exception for past check-in date")
        void testPastCheckInDate() {
            BusinessRuleException exception = assertThrows(BusinessRuleException.class, () -> {
                bookingService.createBooking(
                        testUser, testRoom,
                        LocalDate.now().minusDays(1), // past
                        LocalDate.now().plusDays(1),
                        "Guest", "1234567890", "guest@test.com",
                        2, null, null);
            });

            assertTrue(exception.getMessage().contains("không được trước hôm nay"));
        }

        @Test
        @DisplayName("Should throw exception when check-out before check-in")
        void testCheckOutBeforeCheckIn() {
            LocalDate checkIn = LocalDate.now().plusDays(5);
            LocalDate checkOut = LocalDate.now().plusDays(3); // before check-in

            BusinessRuleException exception = assertThrows(BusinessRuleException.class, () -> {
                bookingService.createBooking(
                        testUser, testRoom,
                        checkIn, checkOut,
                        "Guest", "1234567890", "guest@test.com",
                        2, null, null);
            });

            assertTrue(exception.getMessage().contains("sau ngày nhận phòng"));
        }
    }

    @Nested
    @DisplayName("Check-in/Check-out Tests")
    class CheckInOutTests {

        @Test
        @DisplayName("Should throw exception when checking in unpaid booking")
        void testCheckInUnpaidBooking() {
            testBooking.setStatus(BookingStatus.PENDING_PAYMENT);
            when(bookingRepository.findById(100)).thenReturn(Optional.of(testBooking));

            BusinessRuleException exception = assertThrows(BusinessRuleException.class, () -> {
                bookingService.checkIn(100, adminUser);
            });

            assertTrue(exception.getMessage().contains("đã thanh toán"));
        }

        @Test
        @DisplayName("Should throw exception when check-in too early")
        void testCheckInTooEarly() {
            testBooking.setStatus(BookingStatus.PAID);
            testBooking.setCheckInDate(LocalDate.now().plusDays(5)); // future date
            when(bookingRepository.findById(100)).thenReturn(Optional.of(testBooking));

            BusinessRuleException exception = assertThrows(BusinessRuleException.class, () -> {
                bookingService.checkIn(100, adminUser);
            });

            assertTrue(exception.getMessage().contains("Chưa đến ngày"));
        }

        @Test
        @DisplayName("Should throw exception when checking out without check-in")
        void testCheckOutWithoutCheckIn() {
            testBooking.setStatus(BookingStatus.PAID);
            when(bookingRepository.findById(100)).thenReturn(Optional.of(testBooking));

            BusinessRuleException exception = assertThrows(BusinessRuleException.class, () -> {
                bookingService.checkOut(100, adminUser);
            });

            assertTrue(exception.getMessage().contains("đã nhận phòng"));
        }
    }

    @Nested
    @DisplayName("Refund Calculation Tests")
    class RefundTests {

        @Test
        @DisplayName("Should calculate 100% refund when cancelling 5 days before check-in")
        void testFullRefundFiveDaysBefore() {
            testBooking.setCheckInDate(LocalDate.now().plusDays(10)); // 10 days from now
            testBooking.setTotalPrice(BigDecimal.valueOf(1000));

            when(bookingRepository.findById(100)).thenReturn(Optional.of(testBooking));
            when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

            Booking cancelled = bookingService.cancelBooking(100, testUser);

            assertEquals(100, cancelled.getRefundPercentage());
            assertEquals(BigDecimal.valueOf(1000), cancelled.getRefundAmount());
        }

        @Test
        @DisplayName("Should calculate 50% refund when cancelling 2 days before check-in")
        void testPartialRefundTwoDaysBefore() {
            testBooking.setCheckInDate(LocalDate.now().plusDays(2)); // 2 days from now
            testBooking.setTotalPrice(BigDecimal.valueOf(1000));

            when(bookingRepository.findById(100)).thenReturn(Optional.of(testBooking));
            when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

            Booking cancelled = bookingService.cancelBooking(100, testUser);

            assertEquals(50, cancelled.getRefundPercentage());
            assertEquals(BigDecimal.valueOf(500), cancelled.getRefundAmount());
        }

        @Test
        @DisplayName("Should calculate 0% refund when cancelling on check-in day")
        void testNoRefundOnCheckInDay() {
            testBooking.setCheckInDate(LocalDate.now()); // today
            testBooking.setTotalPrice(BigDecimal.valueOf(1000));

            when(bookingRepository.findById(100)).thenReturn(Optional.of(testBooking));
            when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

            Booking cancelled = bookingService.cancelBooking(100, testUser);

            assertEquals(0, cancelled.getRefundPercentage());
            assertEquals(BigDecimal.ZERO, cancelled.getRefundAmount());
        }
    }

    @Nested
    @DisplayName("Status Transition Tests")
    class StatusTransitionTests {

        @Test
        @DisplayName("Should throw exception for invalid status transition")
        void testInvalidStatusTransition() {
            testBooking.setStatus(BookingStatus.CANCELLED);
            when(bookingRepository.findById(100)).thenReturn(Optional.of(testBooking));

            BusinessRuleException exception = assertThrows(BusinessRuleException.class, () -> {
                bookingService.updateBookingStatus(100, BookingStatus.PAID);
            });

            assertEquals("INVALID_STATE", exception.getCode());
        }

        @Test
        @DisplayName("Should throw exception when marking completed booking as anything")
        void testCannotModifyCompletedBooking() {
            testBooking.setStatus(BookingStatus.COMPLETED);
            when(bookingRepository.findById(100)).thenReturn(Optional.of(testBooking));

            BusinessRuleException exception = assertThrows(BusinessRuleException.class, () -> {
                bookingService.updateBookingStatus(100, BookingStatus.CANCELLED);
            });

            assertTrue(exception.getMessage().contains("hoàn thành"));
        }
    }

    @Nested
    @DisplayName("Payment Confirmation Tests")
    class PaymentConfirmationTests {

        @Test
        @DisplayName("Should reject payment amount that does not match booking total")
        void testConfirmPaymentAmountMismatch() {
            testBooking.setStatus(BookingStatus.PENDING_PAYMENT);
            testBooking.setTotalPrice(BigDecimal.valueOf(900));
            when(bookingRepository.findById(100)).thenReturn(Optional.of(testBooking));

            BusinessRuleException exception = assertThrows(BusinessRuleException.class, () -> {
                bookingService.confirmPayment(100, "TXN-MISMATCH", BigDecimal.valueOf(800));
            });

            assertEquals("PAYMENT_AMOUNT_MISMATCH", exception.getCode());
            verify(bookingRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should confirm payment when amount matches booking total")
        void testConfirmPaymentAmountMatches() {
            testBooking.setStatus(BookingStatus.PENDING_PAYMENT);
            testBooking.setTotalPrice(BigDecimal.valueOf(900));
            when(bookingRepository.findById(100)).thenReturn(Optional.of(testBooking));
            when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

            Booking paid = bookingService.confirmPayment(100, "TXN-OK", BigDecimal.valueOf(900));

            assertEquals(BookingStatus.PAID, paid.getStatus());
            assertNotNull(paid.getPaidAt());
            verify(transitionRepository).save(any());
        }
    }

    @Nested
    @DisplayName("No-Show Tests")
    class NoShowTests {

        @Test
        @DisplayName("Should throw exception when marking no-show too early")
        void testNoShowTooEarly() {
            testBooking.setStatus(BookingStatus.PAID);
            testBooking.setCheckInDate(LocalDate.now().plusDays(5)); // future
            when(bookingRepository.findById(100)).thenReturn(Optional.of(testBooking));

            BusinessRuleException exception = assertThrows(BusinessRuleException.class, () -> {
                bookingService.markAsNoShow(100, adminUser);
            });

            assertTrue(exception.getMessage().contains("sau ngày nhận phòng"));
        }

        @Test
        @DisplayName("Should mark booking as no-show when past checkout date")
        void testMarkNoShowAfterCheckOutDate() {
            testBooking.setStatus(BookingStatus.PAID);
            testBooking.setCheckInDate(LocalDate.now().minusDays(2));
            testBooking.setCheckOutDate(LocalDate.now().minusDays(1)); // yesterday
            when(bookingRepository.findById(100)).thenReturn(Optional.of(testBooking));
            when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);

            Booking noShow = bookingService.markAsNoShow(100, adminUser);

            assertEquals(BookingStatus.NO_SHOW, noShow.getStatus());
        }
    }

    @Nested
    @DisplayName("Customer Dashboard Tests")
    class DashboardTests {

        @Test
        @DisplayName("Should throw exception for null user")
        void testDashboardNullUser() {
            BusinessRuleException exception = assertThrows(BusinessRuleException.class, () -> {
                bookingService.getCustomerDashboard(null, 0, 0);
            });

            assertTrue(exception.getMessage().contains("Cần đăng nhập"));
        }

        @Test
        @DisplayName("Should calculate dashboard statistics correctly")
        void testDashboardCalculation() {
            Booking upcoming = new Booking();
            upcoming.setStatus(BookingStatus.PAID);
            upcoming.setCheckInDate(LocalDate.now().plusDays(5));
            upcoming.setCheckOutDate(LocalDate.now().plusDays(7));
            upcoming.setTotalPrice(BigDecimal.valueOf(900));

            Booking completed = new Booking();
            completed.setStatus(BookingStatus.COMPLETED);
            completed.setCheckOutDate(LocalDate.now().minusDays(5));
            completed.setTotalPrice(BigDecimal.valueOf(1000));

            Booking cancelled = new Booking();
            cancelled.setStatus(BookingStatus.CANCELLED);

            when(bookingRepository.findByUserOrderByCreatedAtDesc(testUser))
                    .thenReturn(List.of(upcoming, completed, cancelled));

            var dashboard = bookingService.getCustomerDashboard(testUser, 3, 5);

            assertEquals(3, dashboard.get("totalBookings"));
            assertEquals(1, dashboard.get("upcomingCount"));
            assertEquals(1, dashboard.get("completedCount"));
            assertEquals(1, dashboard.get("cancelledCount"));
            assertEquals(3, dashboard.get("wishlistCount"));
            assertEquals(5L, dashboard.get("unreadNotifications"));
            assertEquals(BigDecimal.valueOf(1900), dashboard.get("totalSpent"));
        }
    }
}
