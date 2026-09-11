package com.hsf.hotel.booking.api;
import com.hsf.hotel.voucher.repository.VoucherRepository;
import com.hsf.hotel.voucher.model.Voucher;
import com.hsf.hotel.user.repository.UserRepository;
import com.hsf.hotel.room.repository.RoomTypeRepository;
import com.hsf.hotel.room.repository.RoomRepository;
import com.hsf.hotel.room.model.RoomTypeEntity;
import com.hsf.hotel.room.model.Room;
import com.hsf.hotel.booking.repository.BookingRepository;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.hsf.hotel.booking.model.BookingStatus;
import com.hsf.hotel.user.model.UserRole;
import com.hsf.hotel.user.model.User;

import com.hsf.hotel.security.CsrfCookieFilter;
import com.hsf.hotel.booking.service.BookingService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Booking Flow Integration Tests")
class BookingFlowIntegrationTest {
    private static final String CSRF_TOKEN = "test-csrf-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private VoucherRepository voucherRepository;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    private User testUser;
    private User staffUser;
    private User managerUser;
    private User adminUser;
    private Room testRoom;
    private Voucher testVoucher;
    private MockHttpSession userSession;
    private MockHttpSession staffSession;
    private MockHttpSession managerSession;
    private MockHttpSession adminSession;

    private static RequestPostProcessor csrfToken() {
        return request -> {
            request.setCookies(new Cookie(CsrfCookieFilter.COOKIE_NAME, CSRF_TOKEN));
            request.addHeader(CsrfCookieFilter.HEADER_NAME, CSRF_TOKEN);
            return request;
        };
    }

    private static RequestPostProcessor duplicateCsrfCookies() {
        return request -> {
            request.setCookies(
                    new Cookie(CsrfCookieFilter.COOKIE_NAME, "stale-csrf-token"),
                    new Cookie(CsrfCookieFilter.COOKIE_NAME, CSRF_TOKEN));
            request.addHeader(CsrfCookieFilter.HEADER_NAME, CSRF_TOKEN);
            return request;
        };
    }

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User();
        testUser.setUsername("testuser_" + System.currentTimeMillis());
        testUser.setEmail(testUser.getUsername() + "@test.com");
        testUser.setPasswordHash("$2a$10$dummy"); // dummy hash
        testUser.setRole("USER");
        testUser.setEmailVerified(true);
        testUser = userRepository.save(testUser);

        staffUser = new User();
        staffUser.setUsername("staff_" + System.currentTimeMillis());
        staffUser.setEmail(staffUser.getUsername() + "@test.com");
        staffUser.setPasswordHash("$2a$10$dummy");
        staffUser.setRole(UserRole.STAFF);
        staffUser.setEmailVerified(true);
        staffUser = userRepository.save(staffUser);

        managerUser = new User();
        managerUser.setUsername("manager_" + System.currentTimeMillis());
        managerUser.setEmail(managerUser.getUsername() + "@test.com");
        managerUser.setPasswordHash("$2a$10$dummy");
        managerUser.setRole(UserRole.MANAGER);
        managerUser.setEmailVerified(true);
        managerUser = userRepository.save(managerUser);

        // Create admin user
        adminUser = new User();
        adminUser.setUsername("admin_" + System.currentTimeMillis());
        adminUser.setEmail(adminUser.getUsername() + "@test.com");
        adminUser.setPasswordHash("$2a$10$dummy");
        adminUser.setRole("ADMIN");
        adminUser.setEmailVerified(true);
        adminUser = userRepository.save(adminUser);

        // Create test room
        RoomTypeEntity roomType = new RoomTypeEntity();
        roomType.setName("Pool Villa");
        roomType.setDescription("Test villa");
        roomType = roomTypeRepository.save(roomType);

        testRoom = new Room();
        testRoom.setRoomNumber("TEST-" + System.currentTimeMillis());
        testRoom.setRoomType(roomType);
        testRoom.setPricePerNight(BigDecimal.valueOf(450));
        testRoom.setCapacity(4);
        testRoom.setBedrooms(2);
        testRoom.setIsAvailable(true);
        testRoom = roomRepository.save(testRoom);

        // Create test voucher
        testVoucher = new Voucher();
        testVoucher.setCode("TEST20");
        testVoucher.setAmount(BigDecimal.valueOf(20));
        testVoucher.setPercent(true);
        testVoucher.setQuantity(100);
        testVoucher.setUsedCount(0);
        testVoucher.setExpiryDate(LocalDate.now().plusMonths(1));
        testVoucher = voucherRepository.save(testVoucher);

        // Setup sessions
        userSession = new MockHttpSession();
        userSession.setAttribute("user", testUser);

        staffSession = new MockHttpSession();
        staffSession.setAttribute("user", staffUser);

        managerSession = new MockHttpSession();
        managerSession.setAttribute("user", managerUser);

        adminSession = new MockHttpSession();
        adminSession.setAttribute("user", adminUser);
    }

    @Nested
    @DisplayName("Authentication Tests")
    class AuthTests {

        @Test
        @DisplayName("Should return 401 for unauthenticated booking creation")
        void testUnauthenticatedBookingCreation() throws Exception {
            mockMvc.perform(post("/api/v1/bookings")
                    .with(csrfToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "roomId": %d,
                            "checkIn": "%s",
                            "checkOut": "%s",
                            "guestName": "Test Guest"
                        }
                        """.formatted(testRoom.getId(), 
                              LocalDate.now().plusDays(5),
                              LocalDate.now().plusDays(7))))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should allow authenticated access to own bookings")
        void testAuthenticatedAccess() throws Exception {
            mockMvc.perform(get("/api/v1/bookings")
                    .session(userSession))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("Should not authenticate an account before email verification")
        void testRegisterRequiresVerificationBeforeSession() throws Exception {
            String username = "newuser_" + System.currentTimeMillis();

            var result = mockMvc.perform(post("/api/v1/auth/register")
                    .with(csrfToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "username": "%s",
                            "email": "%s@test.com",
                            "password": "Password123",
                            "fullName": "New User"
                        }
                        """.formatted(username, username)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.user.username").value(username))
                    .andExpect(jsonPath("$.data.user.emailVerified").value(false))
                    .andReturn();

            mockMvc.perform(get("/api/v1/auth/session")
                    .with(csrfToken()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.user").doesNotExist());

            User unverified = userRepository.findByUsername(username).orElseThrow();
            MockHttpSession forgedLegacySession = new MockHttpSession();
            forgedLegacySession.setAttribute("user", unverified);
            mockMvc.perform(get("/api/v1/bookings").session(forgedLegacySession))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Booking Creation Tests")
    class BookingCreationTests {

        @Test
        @DisplayName("Should create booking successfully with valid data")
        void testCreateBookingSuccess() throws Exception {
            mockMvc.perform(post("/api/v1/bookings")
                    .session(userSession)
                    .with(csrfToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "roomId": %d,
                            "checkIn": "%s",
                            "checkOut": "%s",
                            "guestName": "Nguyen Van A",
                            "guestEmail": "nguyenvana@email.com",
                            "guestPhone": "0912345678",
                            "guests": 2
                        }
                        """.formatted(testRoom.getId(),
                              LocalDate.now().plusDays(5),
                              LocalDate.now().plusDays(7))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.booking").exists())
                    .andExpect(jsonPath("$.data.booking.status").value("PENDING_PAYMENT"))
                    .andExpect(jsonPath("$.data.reference").value(matchesPattern("NV-\\d+")));
        }

        @Test
        @DisplayName("Should accept CSRF header when browser sends duplicate CSRF cookies")
        void testCreateBookingWithDuplicateCsrfCookies() throws Exception {
            mockMvc.perform(post("/api/v1/bookings")
                    .session(userSession)
                    .with(duplicateCsrfCookies())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "roomId": %d,
                            "checkIn": "%s",
                            "checkOut": "%s",
                            "guestName": "Nguyen Van A",
                            "guestEmail": "nguyenvana@email.com",
                            "guestPhone": "0912345678",
                            "guests": 2
                        }
                        """.formatted(testRoom.getId(),
                              LocalDate.now().plusDays(10),
                              LocalDate.now().plusDays(12))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.booking").exists());
        }

        @Test
        @DisplayName("Should reject booking with missing required fields")
        void testCreateBookingMissingFields() throws Exception {
            mockMvc.perform(post("/api/v1/bookings")
                    .session(userSession)
                    .with(csrfToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "roomId": %d
                        }
                        """.formatted(testRoom.getId())))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject booking with past check-in date")
        void testCreateBookingPastDate() throws Exception {
            mockMvc.perform(post("/api/v1/bookings")
                    .session(userSession)
                    .with(csrfToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "roomId": %d,
                            "checkIn": "%s",
                            "checkOut": "%s",
                            "guestName": "Test Guest"
                        }
                        """.formatted(testRoom.getId(),
                              LocalDate.now().minusDays(1),
                              LocalDate.now().plusDays(1))))
                    .andExpect(status().isUnprocessableEntity());
        }

        @Test
        @DisplayName("Should reject booking with check-out before check-in")
        void testCreateBookingInvalidDates() throws Exception {
            mockMvc.perform(post("/api/v1/bookings")
                    .session(userSession)
                    .with(csrfToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "roomId": %d,
                            "checkIn": "%s",
                            "checkOut": "%s",
                            "guestName": "Test Guest"
                        }
                        """.formatted(testRoom.getId(),
                              LocalDate.now().plusDays(7),
                              LocalDate.now().plusDays(5))))
                    .andExpect(status().isUnprocessableEntity());
        }

        @Test
        @DisplayName("Should reject booking with invalid email")
        void testCreateBookingInvalidEmail() throws Exception {
            mockMvc.perform(post("/api/v1/bookings")
                    .session(userSession)
                    .with(csrfToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "roomId": %d,
                            "checkIn": "%s",
                            "checkOut": "%s",
                            "guestName": "Test Guest",
                            "guestEmail": "invalid-email"
                        }
                        """.formatted(testRoom.getId(),
                              LocalDate.now().plusDays(5),
                              LocalDate.now().plusDays(7))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject booking with guests exceeding capacity")
        void testCreateBookingExceedsCapacity() throws Exception {
            mockMvc.perform(post("/api/v1/bookings")
                    .session(userSession)
                    .with(csrfToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "roomId": %d,
                            "checkIn": "%s",
                            "checkOut": "%s",
                            "guestName": "Test Guest",
                            "guests": 10
                        }
                        """.formatted(testRoom.getId(),
                              LocalDate.now().plusDays(5),
                              LocalDate.now().plusDays(7))))
                    .andExpect(status().isUnprocessableEntity());
        }
    }

    @Nested
    @DisplayName("Voucher Tests")
    class VoucherTests {

        @Test
        @DisplayName("Should validate valid voucher")
        void testValidateValidVoucher() throws Exception {
            mockMvc.perform(get("/api/v1/vouchers/validate")
                    .param("code", "TEST20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.valid").value(true));
        }

        @Test
        @DisplayName("Should reject invalid voucher")
        void testValidateInvalidVoucher() throws Exception {
            mockMvc.perform(get("/api/v1/vouchers/validate")
                    .param("code", "INVALID"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.valid").value(false));
        }
    }

    @Nested
    @DisplayName("Booking Retrieval Tests")
    class BookingRetrievalTests {

        @Test
        @DisplayName("Should get booking by ID for owner")
        void testGetOwnBooking() throws Exception {
            // Create booking first
            var result = mockMvc.perform(post("/api/v1/bookings")
                    .session(userSession)
                    .with(csrfToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "roomId": %d,
                            "checkIn": "%s",
                            "checkOut": "%s",
                            "guestName": "Test Guest"
                        }
                        """.formatted(testRoom.getId(),
                              LocalDate.now().plusDays(5),
                              LocalDate.now().plusDays(7))))
                    .andExpect(status().isOk())
                    .andReturn();

            String response = result.getResponse().getContentAsString();
            Integer bookingId = objectMapper.readTree(response)
                    .path("data").path("booking").path("id").asInt();

            // Get booking
            mockMvc.perform(get("/api/v1/bookings/" + bookingId)
                    .session(userSession))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(bookingId));
        }

        @Test
        @DisplayName("Should reject access to other user's booking")
        void testCannotAccessOtherBooking() throws Exception {
            // Create booking as testUser
            var result = mockMvc.perform(post("/api/v1/bookings")
                    .session(userSession)
                    .with(csrfToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "roomId": %d,
                            "checkIn": "%s",
                            "checkOut": "%s",
                            "guestName": "Test Guest"
                        }
                        """.formatted(testRoom.getId(),
                              LocalDate.now().plusDays(5),
                              LocalDate.now().plusDays(7))))
                    .andExpect(status().isOk())
                    .andReturn();

            String response = result.getResponse().getContentAsString();
            Integer bookingId = objectMapper.readTree(response)
                    .path("data").path("booking").path("id").asInt();

            // Create another user
            User otherUser = new User();
            otherUser.setUsername("other_" + System.currentTimeMillis());
            otherUser.setEmail(otherUser.getUsername() + "@test.com");
            otherUser.setPasswordHash("$2a$10$dummy");
            otherUser.setRole("USER");
            otherUser.setEmailVerified(true);
            otherUser = userRepository.save(otherUser);

            MockHttpSession otherSession = new MockHttpSession();
            otherSession.setAttribute("user", otherUser);

            // Try to access as other user
            mockMvc.perform(get("/api/v1/bookings/" + bookingId)
                    .session(otherSession))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Admin Booking Management Tests")
    class AdminBookingTests {

        @Test
        @DisplayName("Admin should access any booking")
        void testAdminAccessAnyBooking() throws Exception {
            // Create booking as regular user
            var result = mockMvc.perform(post("/api/v1/bookings")
                    .session(userSession)
                    .with(csrfToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "roomId": %d,
                            "checkIn": "%s",
                            "checkOut": "%s",
                            "guestName": "Test Guest"
                        }
                        """.formatted(testRoom.getId(),
                              LocalDate.now().plusDays(5),
                              LocalDate.now().plusDays(7))))
                    .andExpect(status().isOk())
                    .andReturn();

            String response = result.getResponse().getContentAsString();
            Integer bookingId = objectMapper.readTree(response)
                    .path("data").path("booking").path("id").asInt();

            // Access as admin
            mockMvc.perform(get("/api/v1/bookings/" + bookingId)
                    .session(adminSession))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(bookingId));
        }

        @Test
        @DisplayName("Admin should list all bookings")
        void testAdminListAllBookings() throws Exception {
            // Create a booking
            mockMvc.perform(post("/api/v1/bookings")
                    .session(userSession)
                    .with(csrfToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "roomId": %d,
                            "checkIn": "%s",
                            "checkOut": "%s",
                            "guestName": "Test Guest"
                        }
                        """.formatted(testRoom.getId(),
                              LocalDate.now().plusDays(5),
                              LocalDate.now().plusDays(7))))
                    .andExpect(status().isOk());

            // List all bookings as admin
            mockMvc.perform(get("/api/v1/admin/bookings")
                    .session(adminSession))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.bookings").isArray());
        }

        @Test
        @DisplayName("Non-admin should not access admin endpoints")
        void testNonAdminCannotAccessAdmin() throws Exception {
            mockMvc.perform(get("/api/v1/admin/bookings")
                    .session(userSession))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Staff can view operations but cannot manage rooms")
        void testStaffPermissionBoundary() throws Exception {
            mockMvc.perform(get("/api/v1/admin/bookings").session(staffSession))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/v1/admin/rooms").session(staffSession))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Manager can manage rooms but cannot read security audit logs")
        void testManagerPermissionBoundary() throws Exception {
            mockMvc.perform(get("/api/v1/admin/rooms").session(managerSession))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/v1/admin/audit-logs").session(managerSession))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Only admin may confirm a manual payment")
        void testManualPaymentConfirmationRoleBoundary() throws Exception {
            mockMvc.perform(post("/api/v1/payments/999999/complete")
                            .session(userSession).with(csrfToken()).contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post("/api/v1/payments/999999/complete")
                            .session(staffSession).with(csrfToken()).contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post("/api/v1/payments/999999/complete")
                            .session(managerSession).with(csrfToken()).contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post("/api/v1/payments/999999/complete")
                            .session(adminSession).with(csrfToken()).contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Browser clients cannot directly mark their booking paid")
        void testDirectBookingPaymentEndpointIsRemoved() throws Exception {
            mockMvc.perform(post("/api/v1/bookings/999999/payment")
                            .session(userSession).with(csrfToken()).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"amount\":1}"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Payment Endpoint Security Tests")
    class PaymentEndpointSecurityTests {

        @Test
        @DisplayName("Stripe session lookup requires a locally correlated payment owned by caller")
        void testStripeSessionOwnershipUsesLocalPayment() throws Exception {
            var bookingResult = mockMvc.perform(post("/api/v1/bookings")
                            .session(userSession)
                            .with(csrfToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                    "roomId": %d,
                                    "checkIn": "%s",
                                    "checkOut": "%s",
                                    "guestName": "Payment Owner"
                                }
                                """.formatted(testRoom.getId(),
                                    LocalDate.now().plusDays(20),
                                    LocalDate.now().plusDays(22))))
                    .andExpect(status().isOk())
                    .andReturn();
            var bookingJson = objectMapper.readTree(bookingResult.getResponse().getContentAsString())
                    .path("data").path("booking");
            int bookingId = bookingJson.path("id").asInt();
            BigDecimal amount = bookingJson.path("totalPrice").decimalValue();
            String sessionRef = "cs_local_" + System.nanoTime();

            mockMvc.perform(post("/api/v1/payments")
                            .session(userSession)
                            .with(csrfToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                    "bookingId": %d,
                                    "amount": %s,
                                    "method": "CARD",
                                    "transactionRef": "%s"
                                }
                                """.formatted(bookingId, amount.toPlainString(), sessionRef)))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/v1/payments/stripe-session/{sessionId}", sessionRef)
                            .session(userSession))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.bookingId").value(bookingId));

            mockMvc.perform(get("/api/v1/payments/stripe-session/{sessionId}", sessionRef)
                            .session(staffSession))
                    .andExpect(status().isForbidden());

            mockMvc.perform(get("/api/v1/payments/stripe-session/cs_unknown")
                            .session(userSession))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Legacy generic payment-status webhook is not exposed")
        void testGenericWebhookIsRemoved() throws Exception {
            mockMvc.perform(post("/api/v1/payments/webhook/legacy")
                            .session(userSession)
                            .with(csrfToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"intentId\":\"pi_fake\",\"status\":\"succeeded\"}"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("CORS permits the refund idempotency header")
        void testCorsAllowsIdempotencyKey() throws Exception {
            mockMvc.perform(options("/api/v1/payments/1/refund")
                            .header("Origin", "http://localhost:5173")
                            .header("Access-Control-Request-Method", "POST")
                            .header("Access-Control-Request-Headers", "Idempotency-Key,X-XSRF-TOKEN"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Headers",
                            containsString("Idempotency-Key")));
        }
    }

    @Nested
    @DisplayName("Pricing Preview Tests")
    class PricingTests {

        @Test
        @DisplayName("Should return correct pricing preview")
        void testPricingPreview() throws Exception {
            mockMvc.perform(get("/api/v1/bookings/pricing-preview")
                    .param("roomId", testRoom.getId().toString())
                    .param("checkIn", LocalDate.now().plusDays(5).toString())
                    .param("checkOut", LocalDate.now().plusDays(8).toString())) // 3 nights
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.subtotal").exists())
                    .andExpect(jsonPath("$.data.serviceFee").exists())
                    .andExpect(jsonPath("$.data.taxAmount").exists())
                    .andExpect(jsonPath("$.data.total").exists());
        }
    }

    @Nested
    @DisplayName("Availability Tests")
    class AvailabilityTests {

        @Test
        @DisplayName("Should detect double booking conflict")
        void testDoubleBookingPrevention() throws Exception {
            LocalDate checkIn = LocalDate.now().plusDays(10);
            LocalDate checkOut = LocalDate.now().plusDays(12);

            // Create first booking
            mockMvc.perform(post("/api/v1/bookings")
                    .session(userSession)
                    .with(csrfToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "roomId": %d,
                            "checkIn": "%s",
                            "checkOut": "%s",
                            "guestName": "First Guest"
                        }
                        """.formatted(testRoom.getId(), checkIn, checkOut)))
                    .andExpect(status().isOk());

            // Try to create overlapping booking
            mockMvc.perform(post("/api/v1/bookings")
                    .session(userSession)
                    .with(csrfToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "roomId": %d,
                            "checkIn": "%s",
                            "checkOut": "%s",
                            "guestName": "Second Guest"
                        }
                        """.formatted(testRoom.getId(), checkIn, checkOut)))
                    .andExpect(status().isUnprocessableEntity());
        }
    }
}
