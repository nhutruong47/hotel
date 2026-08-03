package com.hsf.hotel.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hsf.hotel.model.*;
import com.hsf.hotel.model.BookingStatus;
import com.hsf.hotel.model.UserRole;
import com.hsf.hotel.repository.*;
import com.hsf.hotel.security.CsrfCookieFilter;
import com.hsf.hotel.service.BookingService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
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

    private User testUser;
    private User adminUser;
    private Room testRoom;
    private Voucher testVoucher;
    private MockHttpSession userSession;
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
        @DisplayName("Should create an authenticated session after registration")
        void testRegisterCreatesSession() throws Exception {
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

            MockHttpSession registeredSession = (MockHttpSession) result.getRequest().getSession(false);

            mockMvc.perform(get("/api/v1/auth/session")
                    .session(registeredSession))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.user.username").value(username));
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
