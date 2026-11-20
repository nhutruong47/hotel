package com.hsf.hotel.it;

import com.hsf.hotel.booking.model.Booking;
import com.hsf.hotel.booking.model.BookingStatus;
import com.hsf.hotel.booking.repository.BookingRepository;
import com.hsf.hotel.booking.service.BookingService;
import com.hsf.hotel.notification.service.NotificationProducer;
import com.hsf.hotel.payment.repository.PaymentRepository;
import com.hsf.hotel.room.model.Room;
import com.hsf.hotel.room.model.RoomTypeEntity;
import com.hsf.hotel.room.repository.RoomRepository;
import com.hsf.hotel.room.repository.RoomTypeRepository;
import com.hsf.hotel.user.model.User;
import com.hsf.hotel.user.model.UserRole;
import com.hsf.hotel.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * A payment provider delivering the same webhook several times (retries, or two
 * deliveries racing) must produce exactly one payment and one PAID transition.
 */
@AutoConfigureMockMvc
@TestPropertySource(properties = "SEPAY_API_KEY=it-sepay-key")
@DisplayName("Duplicate payment webhook handling on PostgreSQL")
@RequiresPostgres
class DuplicatePaymentWebhookIT extends AbstractPostgresIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private BookingService bookingService;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private RoomTypeRepository roomTypeRepository;

    @MockBean private NotificationProducer notificationProducer;

    @Test
    @DisplayName("Same SePay transaction delivered 5x concurrently -> one payment, booking PAID")
    void concurrentDuplicateDeliveries() throws Exception {
        Booking booking = pendingBooking();
        String txRef = "IT-SEPAY-" + System.nanoTime();
        String body = """
                {"id": 1, "gateway": "VCB", "transferType": "in", "content": "NV-%d thanh toan",
                 "transferAmount": %s, "referenceCode": "%s", "accountNumber": "0000"}
                """.formatted(booking.getId(), booking.getTotalPrice().toPlainString(), txRef);

        int deliveries = 5;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(deliveries);
        try {
            List<Future<Integer>> statuses = new ArrayList<>();
            for (int i = 0; i < deliveries; i++) {
                statuses.add(pool.submit(() -> {
                    start.await(10, TimeUnit.SECONDS);
                    return mockMvc.perform(post("/api/v1/payments/webhook/sepay")
                                    .header("Authorization", "Apikey it-sepay-key")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(body))
                            .andReturn().getResponse().getStatus();
                }));
            }
            start.countDown();
            for (Future<Integer> s : statuses) {
                // 200 (processed / idempotent replay) or 409 (lost the race on the unique
                // transaction_ref; the provider retries and then gets an idempotent 200).
                assertThat(s.get(60, TimeUnit.SECONDS)).isIn(200, 409);
            }
        } finally {
            pool.shutdownNow();
        }

        long payments = paymentRepository.findAll().stream()
                .filter(p -> txRef.equals(p.getTransactionRef()))
                .count();
        assertThat(payments).isEqualTo(1);
        assertThat(bookingRepository.findById(booking.getId()).orElseThrow().getStatus())
                .isEqualTo(BookingStatus.PAID);
    }

    private Booking pendingBooking() {
        long s = System.nanoTime();
        User u = new User();
        u.setUsername("it_pay_" + s);
        u.setEmail("it_pay_" + s + "@it.local");
        u.setPasswordHash("$2a$10$dummy");
        u.setRole(UserRole.USER);
        u.setEmailVerified(true);
        u = userRepository.save(u);

        RoomTypeEntity type = new RoomTypeEntity();
        type.setName("IT pay type " + s);
        type.setDescription("integration");
        type = roomTypeRepository.save(type);
        Room r = new Room();
        r.setRoomNumber("IT-PAY-" + s);
        r.setRoomType(type);
        r.setPricePerNight(BigDecimal.valueOf(1_000_000));
        r.setCapacity(4);
        r.setBedrooms(2);
        r.setIsAvailable(true);
        r = roomRepository.save(r);

        LocalDate in = LocalDate.now().plusDays(90);
        return bookingService.createBooking(u, r, in, in.plusDays(2),
                u.getUsername(), "0900000000", u.getEmail(), 2, null, null);
    }
}
