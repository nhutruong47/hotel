package com.hsf.hotel.booking.service;

import com.hsf.hotel.exception.BusinessRuleException;
import com.hsf.hotel.notification.service.NotificationProducer;
import com.hsf.hotel.room.model.Room;
import com.hsf.hotel.room.model.RoomTypeEntity;
import com.hsf.hotel.room.repository.RoomRepository;
import com.hsf.hotel.room.repository.RoomTypeRepository;
import com.hsf.hotel.user.model.User;
import com.hsf.hotel.user.model.UserRole;
import com.hsf.hotel.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Regression tests for P0-02: a day-use booking (checkIn == checkOut) used to be
 * an empty range for the overlap query and could double-book a room. Day-use now
 * occupies the full calendar day [D, D+1) — see {@link com.hsf.hotel.booking.model.Occupancy}.
 * Runs the real JPQL against the H2 test database.
 */
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DayUseOverlapIntegrationTest {

    @Autowired private BookingService bookingService;
    @Autowired private UserRepository userRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private RoomTypeRepository roomTypeRepository;

    @MockBean private NotificationProducer notificationProducer;

    private User guestA;
    private User guestB;
    private Room room;
    private LocalDate day;

    @BeforeEach
    void setUp() {
        long suffix = System.nanoTime();
        guestA = createUser("dayuse_a_" + suffix);
        guestB = createUser("dayuse_b_" + suffix);
        room = createRoom("DAYUSE-" + suffix);
        day = LocalDate.now().plusDays(40);
    }

    @Test
    @DisplayName("Two day-use bookings on the same day: second is rejected")
    void dayUseVersusDayUse() {
        book(guestA, day, day);
        assertUnavailable(() -> book(guestB, day, day));
    }

    @Test
    @DisplayName("Day-use on a day already covered by an overnight stay is rejected")
    void dayUseInsideOvernightStay() {
        book(guestA, day.minusDays(1), day.plusDays(1));
        assertUnavailable(() -> book(guestB, day, day));
    }

    @Test
    @DisplayName("Overnight stay starting on an existing day-use day is rejected")
    void overnightStartingOnDayUse() {
        book(guestA, day, day);
        assertUnavailable(() -> book(guestB, day, day.plusDays(2)));
    }

    @Test
    @DisplayName("Overnight stay that spans an existing day-use day is rejected")
    void overnightSpanningDayUse() {
        book(guestA, day, day);
        assertUnavailable(() -> book(guestB, day.minusDays(2), day.plusDays(1)));
    }

    @Test
    @DisplayName("Day-use on the next day after a day-use is allowed")
    void dayUseOnAdjacentDays() {
        book(guestA, day, day);
        book(guestB, day.plusDays(1), day.plusDays(1));
    }

    @Test
    @DisplayName("Overnight stay checking in the day after a day-use is allowed")
    void overnightAfterDayUse() {
        book(guestA, day, day);
        book(guestB, day.plusDays(1), day.plusDays(3));
    }

    @Test
    @DisplayName("Day-use on the check-out day of an overnight stay is allowed (turnover day) [CẦN XÁC NHẬN]")
    void dayUseOnCheckoutDay() {
        book(guestA, day.minusDays(2), day);
        book(guestB, day, day);
    }

    @Test
    @DisplayName("isRoomAvailable reports a booked day-use day as unavailable")
    void availabilityCheckSeesDayUse() {
        book(guestA, day, day);
        assertThat(bookingService.isRoomAvailable(room, day, day)).isFalse();
        assertThat(bookingService.isRoomAvailable(room, day.minusDays(1), day.plusDays(1))).isFalse();
        assertThat(bookingService.isRoomAvailable(room, day.plusDays(1), day.plusDays(2))).isTrue();
    }

    private void book(User user, LocalDate checkIn, LocalDate checkOut) {
        bookingService.createBooking(user, room, checkIn, checkOut,
                user.getUsername(), "0900000000", user.getEmail(), 2, null, null);
    }

    private void assertUnavailable(Runnable attempt) {
        assertThatThrownBy(attempt::run)
                .isInstanceOf(BusinessRuleException.class)
                .extracting(ex -> ((BusinessRuleException) ex).getCode())
                .isEqualTo("ROOM_UNAVAILABLE");
    }

    private User createUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@test.local");
        user.setPasswordHash("$2a$10$dummy");
        user.setRole(UserRole.USER);
        user.setEmailVerified(true);
        return userRepository.save(user);
    }

    private Room createRoom(String number) {
        RoomTypeEntity type = new RoomTypeEntity();
        type.setName("Day-use type " + number);
        type.setDescription("Day-use overlap integration test");
        type = roomTypeRepository.save(type);

        Room r = new Room();
        r.setRoomNumber(number);
        r.setRoomType(type);
        r.setPricePerNight(BigDecimal.valueOf(1_000_000));
        r.setCapacity(4);
        r.setBedrooms(2);
        r.setIsAvailable(true);
        return roomRepository.save(r);
    }
}
