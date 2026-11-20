package com.hsf.hotel.architecture;

import com.hsf.hotel.booking.model.Booking;
import com.hsf.hotel.booking.model.BookingStatus;
import com.hsf.hotel.booking.model.BookingStatusTransition;
import com.hsf.hotel.booking.repository.BookingRepository;
import com.hsf.hotel.booking.repository.BookingStatusTransitionRepository;
import com.hsf.hotel.contact.model.ContactMessage;
import com.hsf.hotel.contact.repository.ContactMessageRepository;
import com.hsf.hotel.payment.model.Payment;
import com.hsf.hotel.payment.repository.PaymentRepository;
import com.hsf.hotel.review.model.Review;
import com.hsf.hotel.review.repository.ReviewRepository;
import com.hsf.hotel.room.model.Room;
import com.hsf.hotel.room.model.RoomTypeEntity;
import com.hsf.hotel.room.model.VillaMaintenance;
import com.hsf.hotel.room.repository.RoomRepository;
import com.hsf.hotel.room.repository.RoomTypeRepository;
import com.hsf.hotel.room.repository.VillaMaintenanceRepository;
import com.hsf.hotel.user.model.User;
import com.hsf.hotel.user.model.UserRole;
import com.hsf.hotel.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * P0-04 regression: read endpoints must not serialize JPA entities.
 *
 * <p>Runs exactly like production — {@code spring.jpa.open-in-view=false} and
 * <b>no</b> test-managed transaction around the request (the existing
 * {@code @Transactional} MockMvc tests keep a session open and therefore hide
 * {@code LazyInitializationException}). Also asserts that sensitive fields the
 * entities used to leak never appear in the JSON.
 */
@SpringBootTest(properties = {
        "spring.jpa.open-in-view=false",
        "spring.datasource.url=jdbc:h2:mem:noentityleak;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class NoEntityLeakIntegrationTest {

    private static final String BANK_ACCOUNT = "9704-0000-SECRET-ACCT";
    private static final String GATEWAY_RAW = "gateway-raw-SECRET-payload";
    private static final String PASSWORD_HASH = "$2a$10$SECRETHASHvalue";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private RoomTypeRepository roomTypeRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private BookingStatusTransitionRepository transitionRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private ContactMessageRepository contactRepository;
    @Autowired private ReviewRepository reviewRepository;
    @Autowired private VillaMaintenanceRepository maintenanceRepository;

    @MockBean private RabbitTemplate rabbitTemplate;

    private User guest;
    private User admin;
    private Booking booking;

    @BeforeEach
    void seed() {
        long s = System.nanoTime();
        guest = user("leak_guest_" + s, UserRole.USER);
        admin = user("leak_admin_" + s, UserRole.ADMIN);

        RoomTypeEntity type = new RoomTypeEntity();
        type.setName("Leak type " + s);
        type.setDescription("lazy");
        type = roomTypeRepository.save(type);
        Room room = new Room();
        room.setRoomNumber("LEAK-" + s);
        room.setRoomType(type);
        room.setPricePerNight(BigDecimal.valueOf(1_000_000));
        room.setCapacity(4);
        room.setBedrooms(2);
        room.setIsAvailable(true);
        room = roomRepository.save(room);

        booking = new Booking();
        booking.setUser(guest);
        booking.setRoom(room);
        booking.setCheckInDate(LocalDate.now().plusDays(10));
        booking.setCheckOutDate(LocalDate.now().plusDays(12));
        booking.setStatus(BookingStatus.PAID);
        booking.setTotalPrice(BigDecimal.valueOf(2_000_000));
        booking.setRefundAmount(BigDecimal.valueOf(1));
        booking.setGuestName("Guest " + s);
        booking.setGuestEmail("guest@test.local");
        booking.setRefundAccountNumber(BANK_ACCOUNT);
        booking.setPaymentDeadline(LocalDateTime.now().plusMinutes(15));
        booking = bookingRepository.save(booking);

        transitionRepository.save(new BookingStatusTransition(
                booking, BookingStatus.PENDING_PAYMENT, BookingStatus.PAID, admin, "paid"));

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(BigDecimal.valueOf(2_000_000));
        payment.setStatus(Payment.PaymentStatus.PAID);
        payment.setTransactionRef("LEAK-TX-" + s);
        payment.setRawResponse(GATEWAY_RAW);
        paymentRepository.save(payment);

        ContactMessage contact = new ContactMessage();
        contact.setName("Visitor");
        contact.setEmail("visitor@test.local");
        contact.setSubject("Hello");
        contact.setMessage("Question");
        contactRepository.save(contact);

        Review review = new Review();
        review.setUser(guest);
        review.setRoom(room);
        review.setRating(5);
        review.setComment("Great stay");
        reviewRepository.save(review);

        maintenanceRepository.save(new VillaMaintenance(room,
                LocalDate.now().plusDays(30), LocalDate.now().plusDays(31), "Pool cleaning", admin));
    }

    @Test
    @DisplayName("Admin booking list, dashboard and refunds serialize without lazy errors or bank data")
    void adminBookingEndpoints() throws Exception {
        assertSafe(call("/api/v1/admin/bookings", admin), "\"roomNumber\":\"LEAK-");
        assertSafe(call("/api/v1/admin/dashboard", admin), "recentBookings");
        assertSafe(call("/api/v1/admin/refunds", admin), "\"refundAmount\"");
    }

    @Test
    @DisplayName("Payments for a booking omit raw gateway response")
    void paymentsByBooking() throws Exception {
        assertSafe(call("/api/v1/payments/booking/" + booking.getId(), admin), "LEAK-TX-");
    }

    @Test
    @DisplayName("Booking timeline returns transition rows, not entities")
    void bookingTimeline() throws Exception {
        assertSafe(call("/api/v1/bookings/" + booking.getId() + "/timeline", guest), "\"toStatus\":\"PAID\"");
    }

    @Test
    @DisplayName("Guest booking list and account dashboard nextStay serialize safely")
    void guestEndpoints() throws Exception {
        assertSafe(call("/api/v1/bookings", guest), "\"roomNumber\":\"LEAK-");
        assertSafe(call("/api/v1/account/dashboard", guest), "nextStay");
    }

    @Test
    @DisplayName("Admin contacts, reviews and maintenances serialize safely")
    void adminSupportEndpoints() throws Exception {
        assertSafe(call("/api/v1/admin/contacts", admin), "visitor@test.local");
        assertSafe(call("/api/v1/admin/reviews", admin), "Great stay");
        assertSafe(call("/api/v1/admin/maintenances", admin), "Pool cleaning");
    }

    private String call(String path, User as) throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", as);
        MvcResult result = mockMvc.perform(get(path).session(session)).andReturn();
        String body = result.getResponse().getContentAsString();
        assertThat(result.getResponse().getStatus())
                .as("GET %s -> %s", path, body)
                .isEqualTo(200);
        return body;
    }

    private static void assertSafe(String json, String expectedFragment) {
        assertThat(json).contains(expectedFragment);
        assertThat(json).doesNotContain(BANK_ACCOUNT);
        assertThat(json).doesNotContain(GATEWAY_RAW);
        assertThat(json).doesNotContain(PASSWORD_HASH);
        assertThat(json).doesNotContain("hibernateLazyInitializer");
    }

    private User user(String username, UserRole role) {
        User u = new User();
        u.setUsername(username);
        u.setEmail(username + "@test.local");
        u.setPasswordHash(PASSWORD_HASH);
        u.setRole(role);
        u.setEmailVerified(true);
        return userRepository.save(u);
    }
}
