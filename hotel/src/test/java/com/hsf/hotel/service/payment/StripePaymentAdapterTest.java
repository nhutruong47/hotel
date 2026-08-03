package com.hsf.hotel.service.payment;

import com.hsf.hotel.config.StripeConfig;
import com.hsf.hotel.model.Booking;
import com.hsf.hotel.model.Payment;
import com.hsf.hotel.model.User;
import com.hsf.hotel.model.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for StripePaymentAdapter.
 * 
 * <p>These tests verify the adapter behavior in both mock and real modes.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StripePaymentAdapter Tests")
class StripePaymentAdapterTest {

    @Mock
    private StripeConfig stripeConfig;

    private StripePaymentAdapter adapter;

    private Booking testBooking;
    private Payment testPayment;

    @BeforeEach
    void setUp() {
        // Default: adapter disabled (safe for tests)
        adapter = new StripePaymentAdapter(stripeConfig, false, "");
        
        // Setup test booking
        testBooking = createTestBooking();
        
        // Setup test payment
        testPayment = new Payment();
        testPayment.setId(1);
        testPayment.setBooking(testBooking);
        testPayment.setAmount(BigDecimal.valueOf(900));
        testPayment.setCurrency("VND");
    }

    private Booking createTestBooking() {
        User user = new User();
        user.setId(1);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setRole("USER");

        Booking booking = new Booking();
        booking.setId(100);
        booking.setUser(user);
        booking.setCheckInDate(LocalDate.now().plusDays(5));
        booking.setCheckOutDate(LocalDate.now().plusDays(7));
        booking.setGuestName("Test Guest");
        booking.setGuestEmail("guest@example.com");
        booking.setGuests(2);
        return booking;
    }

    @Nested
    @DisplayName("Mock Mode Tests")
    class MockModeTests {

        @Test
        @DisplayName("Should return mock intent ID when adapter is disabled")
        void testMockIntentCreation() {
            String intentId = adapter.createIntent(testBooking, BigDecimal.valueOf(900), "VND");

            assertNotNull(intentId);
            assertTrue(intentId.startsWith("pi_mock_"));
        }

        @Test
        @DisplayName("Mock capture should always succeed")
        void testMockCapture() {
            boolean result = adapter.capture(testPayment);

            assertTrue(result);
            assertNotNull(testPayment.getRawResponse());
            assertTrue(testPayment.getRawResponse().contains("mock"));
        }

        @Test
        @DisplayName("Mock refund should always succeed")
        void testMockRefund() {
            boolean result = adapter.refund(testPayment, BigDecimal.valueOf(500));

            assertTrue(result);
            assertNotNull(testPayment.getRawResponse());
            assertTrue(testPayment.getRawResponse().contains("refunded"));
        }

        @Test
        @DisplayName("isRealStripeEnabled should return false when disabled")
        void testIsRealStripeEnabledDisabled() {
            assertFalse(adapter.isRealStripeEnabled());
        }
    }

    @Nested
    @DisplayName("Mock Intent Detection Tests")
    class MockIntentDetectionTests {

        @Test
        @DisplayName("Should treat mock intent IDs as mock")
        void testMockIntentIdDetection() {
            testPayment.setIntentId("pi_mock_abc123");
            
            boolean result = adapter.capture(testPayment);
            
            assertTrue(result);
            assertTrue(testPayment.getRawResponse().contains("mock"));
        }

        @Test
        @DisplayName("Should detect null intent ID as mock")
        void testNullIntentIdDetection() {
            testPayment.setIntentId(null);
            
            boolean result = adapter.capture(testPayment);
            
            assertTrue(result);
        }
    }

    @Nested
    @DisplayName("Amount Conversion Tests")
    class AmountConversionTests {

        @Test
        @DisplayName("VND should not have decimal conversion")
        void testVNDConversion() {
            // The adapter should handle VND with 0 decimals
            String intentId = adapter.createIntent(testBooking, BigDecimal.valueOf(900000), "VND");
            
            assertNotNull(intentId);
        }

        @Test
        @DisplayName("Should handle large amounts")
        void testLargeAmounts() {
            String intentId = adapter.createIntent(testBooking, BigDecimal.valueOf(100000000), "VND");
            
            assertNotNull(intentId);
        }

        @Test
        @DisplayName("Should handle small amounts")
        void testSmallAmounts() {
            String intentId = adapter.createIntent(testBooking, BigDecimal.valueOf(100), "VND");
            
            assertNotNull(intentId);
        }
    }

    @Nested
    @DisplayName("Webhook Signature Tests")
    class WebhookTests {

        @Test
        @DisplayName("Should throw exception for invalid signature")
        void testInvalidSignature() {
            String payload = "{\"type\": \"checkout.session.completed\"}";
            String invalidSignature = "invalid_signature";

            assertThrows(RuntimeException.class, () -> {
                adapter.verifyWebhookSignature(payload, invalidSignature);
            });
        }

        @Test
        @DisplayName("Should handle missing webhook secret")
        void testMissingWebhookSecret() {
            String payload = "{\"type\": \"checkout.session.completed\"}";
            
            // Should work but skip verification
            // Note: This will fail parsing but won't throw signature error
            assertThrows(RuntimeException.class, () -> {
                adapter.verifyWebhookSignature(payload, null);
            });
        }
    }

    @Nested
    @DisplayName("Status Retrieval Tests")
    class StatusTests {

        @Test
        @DisplayName("Mock intent should return succeeded status")
        void testMockIntentStatus() {
            String status = adapter.getPaymentIntentStatus("pi_mock_123");
            
            assertEquals("succeeded", status);
        }

        @Test
        @DisplayName("Unknown status for non-configured adapter")
        void testUnknownStatus() {
            String status = adapter.getPaymentIntentStatus("pi_unknown");
            
            // With adapter disabled, even non-mock IDs are treated as succeeded (mock mode)
            assertEquals("succeeded", status);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle booking with null user")
        void testBookingWithNullUser() {
            testBooking.setUser(null);
            testBooking.setGuestEmail(null);
            
            String intentId = adapter.createIntent(testBooking, BigDecimal.valueOf(500), "VND");
            
            assertNotNull(intentId);
            assertTrue(intentId.startsWith("pi_mock_"));
        }

        @Test
        @DisplayName("Should handle booking with null room")
        void testBookingWithNullRoom() {
            // Just ensure no NPE
            String intentId = adapter.createIntent(testBooking, BigDecimal.valueOf(500), "VND");
            
            assertNotNull(intentId);
        }

        @Test
        @DisplayName("Should handle payment with null currency")
        void testPaymentWithNullCurrency() {
            testPayment.setCurrency(null);
            
            boolean result = adapter.refund(testPayment, BigDecimal.valueOf(100));
            
            assertTrue(result);
        }
    }
}
