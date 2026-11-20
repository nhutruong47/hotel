package com.hsf.hotel.it;

import com.hsf.hotel.booking.model.Booking;
import com.hsf.hotel.booking.model.BookingStatus;
import com.hsf.hotel.booking.repository.BookingRepository;
import com.hsf.hotel.booking.service.BookingService;
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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Double-booking guarantees on real PostgreSQL:
 * <ul>
 *   <li>V13 exclusion constraint rejects overlaps even when the service layer is bypassed;</li>
 *   <li>N concurrent booking requests for the same room/dates yield exactly one hold.</li>
 * </ul>
 */
@DisplayName("Booking integrity on PostgreSQL")
@RequiresPostgres
class BookingIntegrityIT extends AbstractPostgresIT {

    private static final int CONCURRENT_REQUESTS = 20;

    @Autowired private BookingService bookingService;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private RoomTypeRepository roomTypeRepository;

    @MockBean private NotificationProducer notificationProducer;

    private User guest;
    private Room room;
    private LocalDate base;

    @BeforeEach
    void setUp() {
        long s = System.nanoTime();
        guest = user("it_guest_" + s);
        room = room("IT-" + s);
        base = LocalDate.now().plusDays(60);
    }

    @Test
    @DisplayName("DB rejects an overlapping active booking inserted without the service")
    void exclusionConstraintRejectsOverlap() {
        bookingRepository.saveAndFlush(rawBooking(base, base.plusDays(3), BookingStatus.PAID));

        assertThatThrownBy(() ->
                bookingRepository.saveAndFlush(rawBooking(base.plusDays(1), base.plusDays(2), BookingStatus.PENDING_PAYMENT)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasStackTraceContaining("bookings_no_overlap");
    }

    @Test
    @DisplayName("DB treats day-use as occupying the whole day")
    void exclusionConstraintCoversDayUse() {
        bookingRepository.saveAndFlush(rawBooking(base, base, BookingStatus.PAID));

        assertThatThrownBy(() -> bookingRepository.saveAndFlush(rawBooking(base, base, BookingStatus.PAID)))
                .isInstanceOf(DataIntegrityViolationException.class);
        // Adjacent stays are fine: check-out day of one == check-in day of the next.
        bookingRepository.saveAndFlush(rawBooking(base.plusDays(1), base.plusDays(3), BookingStatus.PAID));
    }

    @Test
    @DisplayName("Released inventory (CANCELLED/EXPIRED/NO_SHOW) does not block new bookings")
    void releasedBookingsDoNotBlock() {
        bookingRepository.saveAndFlush(rawBooking(base, base.plusDays(2), BookingStatus.CANCELLED));
        bookingRepository.saveAndFlush(rawBooking(base, base.plusDays(2), BookingStatus.EXPIRED));
        bookingRepository.saveAndFlush(rawBooking(base, base.plusDays(2), BookingStatus.PAID));
    }

    @Test
    @DisplayName("DB rejects check-out before check-in")
    void dateOrderCheck() {
        assertThatThrownBy(() -> bookingRepository.saveAndFlush(rawBooking(base.plusDays(2), base, BookingStatus.PAID)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasStackTraceContaining("chk_bookings_date_order");
    }

    @Test
    @DisplayName("20 concurrent requests for the same room and dates: exactly one succeeds")
    void concurrentBookingRequests() throws Exception {
        List<User> guests = new ArrayList<>();
        for (int i = 0; i < CONCURRENT_REQUESTS; i++) {
            guests.add(user("it_race_" + i + "_" + System.nanoTime()));
        }
        LocalDate checkIn = base.plusDays(10);
        LocalDate checkOut = checkIn.plusDays(2);

        CountDownLatch ready = new CountDownLatch(CONCURRENT_REQUESTS);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENT_REQUESTS);
        try {
            List<Future<Boolean>> results = new ArrayList<>();
            for (User g : guests) {
                results.add(pool.submit(() -> {
                    ready.countDown();
                    start.await(10, TimeUnit.SECONDS);
                    try {
                        bookingService.createBooking(g, room, checkIn, checkOut,
                                g.getUsername(), "0900000000", g.getEmail(), 2, null, null);
                        return true;
                    } catch (BusinessRuleException ex) {
                        assertThat(ex.getCode()).isEqualTo("ROOM_UNAVAILABLE");
                        return false;
                    } catch (DataIntegrityViolationException ex) {
                        // Defence in depth: the exclusion constraint caught it.
                        return false;
                    }
                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            int successes = 0;
            for (Future<Boolean> f : results) {
                if (f.get(60, TimeUnit.SECONDS)) successes++;
            }
            assertThat(successes).isEqualTo(1);
            long active = bookingRepository.findAllByRoomId(room.getId()).stream()
                    .filter(b -> b.getStatus() == BookingStatus.PENDING_PAYMENT)
                    .count();
            assertThat(active).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }

    private Booking rawBooking(LocalDate in, LocalDate out, BookingStatus status) {
        Booking b = new Booking();
        b.setUser(guest);
        b.setRoom(room);
        b.setCheckInDate(in);
        b.setCheckOutDate(out);
        b.setStatus(status);
        b.setTotalPrice(BigDecimal.valueOf(1_000_000));
        b.setGuestName(guest.getUsername());
        b.setPaymentDeadline(LocalDateTime.now().plusMinutes(15));
        return b;
    }

    private User user(String username) {
        User u = new User();
        u.setUsername(username);
        u.setEmail(username + "@it.local");
        u.setPasswordHash("$2a$10$dummy");
        u.setRole(UserRole.USER);
        u.setEmailVerified(true);
        return userRepository.save(u);
    }

    private Room room(String number) {
        RoomTypeEntity type = new RoomTypeEntity();
        type.setName("IT type " + number);
        type.setDescription("integration");
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
