package com.hsf.hotel.booking.service;

import com.hsf.hotel.booking.model.BookingStatus;
import com.hsf.hotel.booking.repository.BookingRepository;
import com.hsf.hotel.exception.BusinessRuleException;
import com.hsf.hotel.notification.service.NotificationProducer;
import com.hsf.hotel.room.model.Room;
import com.hsf.hotel.room.model.RoomTypeEntity;
import com.hsf.hotel.room.repository.RoomRepository;
import com.hsf.hotel.room.repository.RoomTypeRepository;
import com.hsf.hotel.user.model.User;
import com.hsf.hotel.user.model.UserRole;
import com.hsf.hotel.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BookingConcurrencyIntegrationTest {

    @Autowired private BookingService bookingService;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private RoomTypeRepository roomTypeRepository;

    @MockBean private NotificationProducer notificationProducer;

    @Test
    void onlyOneConcurrentRequestCanHoldTheSameRoomAndDates() throws Exception {
        long suffix = System.nanoTime();
        User firstUser = createUser("race_a_" + suffix);
        User secondUser = createUser("race_b_" + suffix);
        Room room = createRoom("RACE-" + suffix);
        LocalDate checkIn = LocalDate.now().plusDays(30);
        LocalDate checkOut = checkIn.plusDays(2);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> first = executor.submit(() -> attemptBooking(
                    firstUser, room, checkIn, checkOut, ready, start));
            Future<Boolean> second = executor.submit(() -> attemptBooking(
                    secondUser, room, checkIn, checkOut, ready, start));

            assertTrue(ready.await(5, TimeUnit.SECONDS), "Both requests should be ready");
            start.countDown();

            int successes = (first.get(10, TimeUnit.SECONDS) ? 1 : 0)
                    + (second.get(10, TimeUnit.SECONDS) ? 1 : 0);
            assertEquals(1, successes);

            long activeHolds = bookingRepository.findAllByRoomId(room.getId()).stream()
                    .filter(booking -> booking.getStatus() == BookingStatus.PENDING_PAYMENT)
                    .count();
            assertEquals(1, activeHolds);
        } finally {
            executor.shutdownNow();
        }
    }

    private boolean attemptBooking(User user, Room room, LocalDate checkIn, LocalDate checkOut,
                                   CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        start.await(5, TimeUnit.SECONDS);
        try {
            bookingService.createBooking(user, room, checkIn, checkOut,
                    user.getUsername(), "0900000000", user.getEmail(), 2, null, null);
            return true;
        } catch (BusinessRuleException ex) {
            if ("ROOM_UNAVAILABLE".equals(ex.getCode())) {
                return false;
            }
            throw ex;
        }
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
        type.setName("Concurrency type " + number);
        type.setDescription("Concurrency integration test");
        type = roomTypeRepository.save(type);

        Room room = new Room();
        room.setRoomNumber(number);
        room.setRoomType(type);
        room.setPricePerNight(BigDecimal.valueOf(1_000_000));
        room.setCapacity(4);
        room.setBedrooms(2);
        room.setIsAvailable(true);
        return roomRepository.save(room);
    }
}
