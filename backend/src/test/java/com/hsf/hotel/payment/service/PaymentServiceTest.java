package com.hsf.hotel.payment.service;
import com.hsf.hotel.payment.service.PaymentService;
import com.hsf.hotel.notification.service.NotificationProducer;

import com.hsf.hotel.exception.BusinessRuleException;
import com.hsf.hotel.exception.ExternalServiceException;
import com.hsf.hotel.exception.ResourceNotFoundException;
import com.hsf.hotel.booking.model.Booking;
import com.hsf.hotel.booking.model.BookingStatus;
import com.hsf.hotel.booking.model.BookingStatusTransition;
import com.hsf.hotel.payment.model.Payment;
import com.hsf.hotel.payment.model.PaymentRefund;
import com.hsf.hotel.user.model.User;
import com.hsf.hotel.user.model.UserRole;
import com.hsf.hotel.booking.repository.BookingRepository;
import com.hsf.hotel.booking.repository.BookingStatusTransitionRepository;
import com.hsf.hotel.payment.repository.PaymentRepository;
import com.hsf.hotel.payment.repository.PaymentRefundRepository;
import com.hsf.hotel.service.payment.PaymentGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
@DisplayName("PaymentService Tests")
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentRefundRepository paymentRefundRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingStatusTransitionRepository transitionRepository;

    @Mock
    private PaymentGateway paymentGateway;

    @Mock
    private NotificationProducer notificationProducer;

    @InjectMocks
    private PaymentService paymentService;

    private User testUser;
    private Booking testBooking;
    private Payment testPayment;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setRole("USER");

        testBooking = new Booking();
        testBooking.setId(100);
        testBooking.setUser(testUser);
        testBooking.setCheckInDate(LocalDate.now().plusDays(5));
        testBooking.setCheckOutDate(LocalDate.now().plusDays(7));
        testBooking.setStatus(BookingStatus.PENDING_PAYMENT);
        testBooking.setTotalPrice(BigDecimal.valueOf(900));

        testPayment = new Payment();
        testPayment.setId(1);
        testPayment.setBooking(testBooking);
        testPayment.setAmount(BigDecimal.valueOf(900));
        testPayment.setStatus(Payment.PaymentStatus.PENDING);
        testPayment.setPaymentMethod(Payment.PaymentMethod.BANK_TRANSFER);
        testPayment.setPaymentRef("PAY-TEST123");
    }

    @Nested
    @DisplayName("Payment Intent Creation Tests")
    class PaymentIntentTests {

        @Test
        @DisplayName("Should throw exception for null booking")
        void testCreateIntentNullBooking() {
            BusinessRuleException exception = assertThrows(BusinessRuleException.class, () -> {
                paymentService.createPaymentIntent(null, Payment.PaymentMethod.BANK_TRANSFER);
            });

            assertEquals("Booking is required", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception for non-pending booking")
        void testCreateIntentNonPendingBooking() {
            testBooking.setStatus(BookingStatus.PAID);

            BusinessRuleException exception = assertThrows(BusinessRuleException.class, () -> {
                paymentService.createPaymentIntent(testBooking, Payment.PaymentMethod.BANK_TRANSFER);
            });

            assertTrue(exception.getMessage().contains("not in a valid state"));
        }

        @Test
        @DisplayName("Should create payment intent successfully")
        void testCreateIntentSuccess() {
            when(paymentGateway.createIntent(any(), any(), any())).thenReturn("pi_mock_test123");
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
                Payment p = inv.getArgument(0);
                p.setId(1);
                return p;
            });

            Payment result = paymentService.createPaymentIntent(testBooking, Payment.PaymentMethod.BANK_TRANSFER);

            assertNotNull(result);
            assertEquals("pi_mock_test123", result.getIntentId());
            assertEquals(Payment.PaymentStatus.PENDING, result.getStatus());
            assertEquals(Payment.PaymentGateway.MOCK, result.getGateway());
            verify(paymentGateway).createIntent(testBooking, testBooking.getTotalPrice(), "VND");
        }

        @Test
        @DisplayName("Should use specified payment method")
        void testCreateIntentWithDifferentMethod() {
            when(paymentGateway.createIntent(any(), any(), any())).thenReturn("pi_mock_123");
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
                Payment p = inv.getArgument(0);
                p.setId(1);
                return p;
            });

            Payment result = paymentService.createPaymentIntent(testBooking, Payment.PaymentMethod.CARD);

            assertEquals(Payment.PaymentMethod.CARD, result.getPaymentMethod());
        }
    }

    @Nested
    @DisplayName("Webhook Handling Tests")
    class WebhookTests {

        @Test
        @DisplayName("Should throw exception for unknown intent")
        void testWebhookUnknownIntent() {
            when(paymentRepository.findByIntentIdForUpdate("unknown")).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> {
                paymentService.handleWebhook("unknown", "succeeded", "{}");
            });
        }

        @Test
        @DisplayName("Should ignore webhook for already completed payment")
        void testWebhookIgnoredForCompletedPayment() {
            testPayment.setStatus(Payment.PaymentStatus.PAID);
            when(paymentRepository.findByIntentIdForUpdate("pi_mock_123")).thenReturn(Optional.of(testPayment));

            Payment result = paymentService.handleWebhook("pi_mock_123", "succeeded", "{}");

            assertEquals(Payment.PaymentStatus.PAID, result.getStatus());
            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should process successful webhook")
        void testWebhookSuccess() {
            testPayment.setStatus(Payment.PaymentStatus.PENDING);
            when(paymentRepository.findByIntentIdForUpdate("pi_mock_123")).thenReturn(Optional.of(testPayment));
            when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
            when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);

            Payment result = paymentService.handleWebhook("pi_mock_123", "succeeded", "{\"status\":\"succeeded\"}");

            assertEquals(Payment.PaymentStatus.PAID, result.getStatus());
            assertEquals(BookingStatus.PAID, testBooking.getStatus());
            verify(transitionRepository).save(any());
            verify(notificationProducer, times(2)).sendEmailNotification(any());
        }

        @Test
        @DisplayName("Should process failed webhook")
        void testWebhookFailed() {
            testPayment.setStatus(Payment.PaymentStatus.PENDING);
            when(paymentRepository.findByIntentIdForUpdate("pi_mock_123")).thenReturn(Optional.of(testPayment));
            when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

            Payment result = paymentService.handleWebhook("pi_mock_123", "failed", "{\"status\":\"failed\"}");

            assertEquals(Payment.PaymentStatus.FAILED, result.getStatus());
        }

        @Test
        @DisplayName("Should allow a later success after an earlier failed event")
        void testWebhookSuccessAfterFailure() {
            testPayment.setStatus(Payment.PaymentStatus.FAILED);
            when(paymentRepository.findByIntentIdForUpdate("pi_mock_123"))
                    .thenReturn(Optional.of(testPayment));
            when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
            when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);

            Payment result = paymentService.handleWebhook(
                    "pi_mock_123", "succeeded", "{\"status\":\"succeeded\"}");

            assertEquals(Payment.PaymentStatus.PAID, result.getStatus());
            assertEquals(BookingStatus.PAID, testBooking.getStatus());
        }

        @Test
        @DisplayName("Should record but not revive a booking when payment arrives late")
        void testLateWebhookRequiresManualReview() {
            testPayment.setStatus(Payment.PaymentStatus.PENDING);
            testBooking.setStatus(BookingStatus.EXPIRED);
            when(paymentRepository.findByIntentIdForUpdate("pi_mock_123"))
                    .thenReturn(Optional.of(testPayment));
            when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

            Payment result = paymentService.handleWebhook(
                    "pi_mock_123", "succeeded", "{\"status\":\"succeeded\"}");

            assertEquals(Payment.PaymentStatus.PAID, result.getStatus());
            assertEquals(BookingStatus.EXPIRED, testBooking.getStatus());
            assertTrue(result.getNotes().contains("manual review/refund required"));
            verify(bookingRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should correlate checkout completion by Stripe session ID")
        void testCheckoutSessionWebhook() {
            testPayment.setPaymentRef("cs_test_123");
            when(paymentRepository.findByTransactionRefForUpdate("cs_test_123"))
                    .thenReturn(Optional.of(testPayment));
            when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
            when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);

            Payment result = paymentService.handleCheckoutWebhook(
                    "cs_test_123", "pi_stripe_123", "{}");

            assertEquals("pi_stripe_123", result.getIntentId());
            assertEquals(Payment.PaymentStatus.PAID, result.getStatus());
            assertEquals(BookingStatus.PAID, testBooking.getStatus());
        }
    }

    @Nested
    @DisplayName("Payment Creation Tests")
    class PaymentCreationTests {

        @Test
        @DisplayName("Should throw exception for null amount")
        void testCreatePaymentNullAmount() {
            BusinessRuleException exception = assertThrows(BusinessRuleException.class, () -> {
                paymentService.createPayment(testBooking, null, 
                        Payment.PaymentMethod.CASH, "TXN123", "Test payment");
            });

            assertEquals("Số tiền thanh toán phải lớn hơn 0", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception for negative amount")
        void testCreatePaymentNegativeAmount() {
            BusinessRuleException exception = assertThrows(BusinessRuleException.class, () -> {
                paymentService.createPayment(testBooking, BigDecimal.valueOf(-100),
                        Payment.PaymentMethod.CASH, "TXN123", null);
            });

            assertEquals("Số tiền thanh toán phải lớn hơn 0", exception.getMessage());
        }

        @Test
        @DisplayName("Should create payment record successfully")
        void testCreatePaymentSuccess() {
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
                Payment p = inv.getArgument(0);
                p.setId(1);
                return p;
            });

            Payment result = paymentService.createPayment(
                    testBooking,
                    BigDecimal.valueOf(900),
                    Payment.PaymentMethod.BANK_TRANSFER,
                    "TXN456",
                    "Deposit payment"
            );

            assertNotNull(result);
            assertEquals(BigDecimal.valueOf(900), result.getAmount());
            assertEquals(Payment.PaymentStatus.PENDING, result.getStatus());
            assertEquals("Deposit payment", result.getNotes());
        }

        @Test
        @DisplayName("Should generate reference if not provided")
        void testCreatePaymentAutoReference() {
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
                Payment p = inv.getArgument(0);
                p.setId(1);
                return p;
            });

            Payment result = paymentService.createPayment(
                    testBooking,
                    BigDecimal.valueOf(900),
                    Payment.PaymentMethod.CASH,
                    null, // no reference
                    null
            );

            assertNotNull(result.getPaymentRef());
            assertTrue(result.getPaymentRef().startsWith("PAY-"));
        }

        @Test
        @DisplayName("Should reject a payment amount that differs from booking total")
        void testCreatePaymentAmountMismatch() {
            BusinessRuleException exception = assertThrows(BusinessRuleException.class, () ->
                    paymentService.createPayment(testBooking, BigDecimal.valueOf(899),
                            Payment.PaymentMethod.BANK_TRANSFER, "TX-MISMATCH", null));

            assertEquals("PAYMENT_AMOUNT_MISMATCH", exception.getCode());
            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should persist the actual Stripe checkout correlation IDs")
        void testCreateCheckoutPayment() {
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

            Payment result = paymentService.createCheckoutPayment(
                    testBooking, "cs_test_456", "pi_test_456");

            assertEquals("cs_test_456", result.getTransactionRef());
            assertEquals("pi_test_456", result.getIntentId());
            assertEquals(Payment.PaymentGateway.STRIPE, result.getGateway());
            assertEquals(testBooking.getTotalPrice(), result.getAmount());
        }
    }

    @Nested
    @DisplayName("Payment Completion Tests")
    class PaymentCompletionTests {

        @Test
        @DisplayName("Should throw exception for already paid payment")
        void testMarkCompletedAlreadyPaid() {
            testPayment.setStatus(Payment.PaymentStatus.PAID);
            when(paymentRepository.findByIdForUpdate(1)).thenReturn(Optional.of(testPayment));

            Payment result = paymentService.markCompleted(1, testUser);

            assertEquals(Payment.PaymentStatus.PAID, result.getStatus());
            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should mark payment as completed")
        void testMarkCompletedSuccess() {
            testPayment.setStatus(Payment.PaymentStatus.PENDING);
            when(paymentRepository.findByIdForUpdate(1)).thenReturn(Optional.of(testPayment));
            when(bookingRepository.findByIdForUpdate(100)).thenReturn(Optional.of(testBooking));
            when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
            when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);

            Payment result = paymentService.markCompleted(1, testUser);

            assertEquals(Payment.PaymentStatus.PAID, result.getStatus());
            assertEquals(BookingStatus.PAID, testBooking.getStatus());
            assertNotNull(result.getCompletedAt());
        }

        @Test
        @DisplayName("Should update booking status to PAID on completion")
        void testMarkCompletedUpdatesBooking() {
            testPayment.setStatus(Payment.PaymentStatus.PENDING);
            testBooking.setStatus(BookingStatus.PENDING_PAYMENT);
            when(paymentRepository.findByIdForUpdate(1)).thenReturn(Optional.of(testPayment));
            when(bookingRepository.findByIdForUpdate(100)).thenReturn(Optional.of(testBooking));
            when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
            when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);

            paymentService.markCompleted(1, testUser);

            assertEquals(BookingStatus.PAID, testBooking.getStatus());
            assertNotNull(testBooking.getPaidAt());
            verify(transitionRepository).save(any(BookingStatusTransition.class));
        }
    }

    @Nested
    @DisplayName("Payment Failure Tests")
    class PaymentFailureTests {

        @Test
        @DisplayName("Should mark payment as failed")
        void testMarkFailedSuccess() {
            testPayment.setStatus(Payment.PaymentStatus.PENDING);
            when(paymentRepository.findByIdForUpdate(1)).thenReturn(Optional.of(testPayment));
            when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

            Payment result = paymentService.markFailed(1, "Insufficient funds");

            assertEquals(Payment.PaymentStatus.FAILED, result.getStatus());
            assertTrue(result.getNotes().contains("Insufficient funds"));
        }

        @Test
        @DisplayName("Should ignore failed status for already refunded payment")
        void testMarkFailedIgnoredForRefunded() {
            testPayment.setStatus(Payment.PaymentStatus.REFUNDED);
            when(paymentRepository.findByIdForUpdate(1)).thenReturn(Optional.of(testPayment));

            Payment result = paymentService.markFailed(1, "Test reason");

            assertEquals(Payment.PaymentStatus.REFUNDED, result.getStatus());
            verify(paymentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Refund Tests")
    class RefundTests {

        @Test
        @DisplayName("Should throw exception for already fully refunded payment")
        void testRefundAlreadyRefunded() {
            testPayment.setStatus(Payment.PaymentStatus.REFUNDED);
            testPayment.setRefundAmount(BigDecimal.valueOf(900));
            when(paymentRepository.findByIdForUpdate(1)).thenReturn(Optional.of(testPayment));

            assertThrows(BusinessRuleException.class, () ->
                    paymentService.refund(1, BigDecimal.valueOf(100), "Customer request", testUser, "refund-1"));
            verify(paymentGateway, never()).refund(any(), any(), any());
        }

        @Test
        @DisplayName("Should throw exception for unpaid payment")
        void testRefundUnpaidPayment() {
            testPayment.setStatus(Payment.PaymentStatus.PENDING);
            when(paymentRepository.findByIdForUpdate(1)).thenReturn(Optional.of(testPayment));

            BusinessRuleException exception = assertThrows(BusinessRuleException.class, () -> {
                paymentService.refund(1, BigDecimal.valueOf(100), "Test", testUser, "refund-2");
            });

            assertTrue(exception.getMessage().contains("hoàn tiền cho thanh toán đã hoàn thành"));
        }

        @Test
        @DisplayName("Should throw exception for refund amount exceeding paid amount")
        void testRefundOverflow() {
            testPayment.setStatus(Payment.PaymentStatus.PAID);
            testPayment.setAmount(BigDecimal.valueOf(900));
            when(paymentRepository.findByIdForUpdate(1)).thenReturn(Optional.of(testPayment));
            BusinessRuleException exception = assertThrows(BusinessRuleException.class, () -> {
                paymentService.refund(1, BigDecimal.valueOf(1000), "Test", testUser, "refund-3"); // exceeds 900
            });

            assertTrue(exception.getMessage().contains("vượt quá số tiền đã thanh toán"));
        }

        @Test
        @DisplayName("Should process partial refund correctly")
        void testPartialRefund() {
            testPayment.setStatus(Payment.PaymentStatus.PAID);
            testPayment.setAmount(BigDecimal.valueOf(1000));
            testPayment.setRefundAmount(BigDecimal.ZERO);
            when(paymentRepository.findByIdForUpdate(1)).thenReturn(Optional.of(testPayment));
            when(bookingRepository.findByIdForUpdate(100)).thenReturn(Optional.of(testBooking));
            when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
            when(paymentGateway.refund(any(), any(), eq("refund-4"))).thenReturn(true);
            when(bookingRepository.save(any())).thenReturn(testBooking);

            Payment result = paymentService.refund(1, BigDecimal.valueOf(500), "Partial refund", testUser, "refund-4");

            assertEquals(Payment.PaymentStatus.PARTIALLY_REFUNDED, result.getStatus());
            assertEquals(BigDecimal.valueOf(500), result.getRefundAmount());
            verify(paymentGateway).refund(
                    eq(testPayment), eq(BigDecimal.valueOf(500)), eq("refund-4"));
        }

        @Test
        @DisplayName("Should process full refund correctly")
        void testFullRefund() {
            testPayment.setStatus(Payment.PaymentStatus.PAID);
            testPayment.setAmount(BigDecimal.valueOf(1000));
            testPayment.setRefundAmount(BigDecimal.ZERO);
            when(paymentRepository.findByIdForUpdate(1)).thenReturn(Optional.of(testPayment));
            when(bookingRepository.findByIdForUpdate(100)).thenReturn(Optional.of(testBooking));
            when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
            when(paymentGateway.refund(any(), any(), eq("refund-5"))).thenReturn(true);
            when(bookingRepository.save(any())).thenReturn(testBooking);

            Payment result = paymentService.refund(1, BigDecimal.valueOf(1000), "Full refund", testUser, "refund-5");

            assertEquals(Payment.PaymentStatus.REFUNDED, result.getStatus());
            assertEquals(BigDecimal.valueOf(1000), result.getRefundAmount());
            assertNotNull(result.getRefundedAt());
        }

        @Test
        @DisplayName("Should throw exception for negative refund amount")
        void testRefundNegativeAmount() {
            testPayment.setStatus(Payment.PaymentStatus.PAID);

            BusinessRuleException exception = assertThrows(BusinessRuleException.class, () -> {
                paymentService.refund(1, BigDecimal.valueOf(-100), "Test", testUser, "refund-6");
            });

            assertEquals("Số tiền hoàn phải lớn hơn 0", exception.getMessage());
        }

        @Test
        @DisplayName("Should return prior result for a repeated refund idempotency key")
        void testRefundIdempotentRetry() {
            testPayment.setStatus(Payment.PaymentStatus.PARTIALLY_REFUNDED);
            testPayment.setRefundAmount(BigDecimal.valueOf(100));
            PaymentRefund previous = new PaymentRefund();
            previous.setPayment(testPayment);
            previous.setIdempotencyKey("same-refund");
            previous.setAmount(BigDecimal.valueOf(100));
            previous.setStatus(PaymentRefund.RefundStatus.SUCCEEDED);
            when(paymentRepository.findByIdForUpdate(1)).thenReturn(Optional.of(testPayment));
            when(paymentRefundRepository.findByPaymentIdAndIdempotencyKey(1, "same-refund"))
                    .thenReturn(Optional.of(previous));

            Payment result = paymentService.refund(
                    1, BigDecimal.valueOf(100), "Retry", testUser, "same-refund");

            assertSame(testPayment, result);
            verify(paymentGateway, never()).refund(any(), any(), any());
        }

        @Test
        @DisplayName("Should preserve local totals when gateway rejects refund")
        void testRefundGatewayFailure() {
            testPayment.setStatus(Payment.PaymentStatus.PAID);
            testPayment.setRefundAmount(BigDecimal.ZERO);
            when(paymentRepository.findByIdForUpdate(1)).thenReturn(Optional.of(testPayment));
            when(bookingRepository.findByIdForUpdate(100)).thenReturn(Optional.of(testBooking));
            when(paymentGateway.refund(testPayment, BigDecimal.valueOf(100), "failed-refund"))
                    .thenReturn(false);

            assertThrows(ExternalServiceException.class, () -> paymentService.refund(
                    1, BigDecimal.valueOf(100), "Gateway failure", testUser, "failed-refund"));

            assertEquals(Payment.PaymentStatus.PAID, testPayment.getStatus());
            assertEquals(BigDecimal.ZERO, testPayment.getRefundAmount());
            verify(paymentRepository, never()).save(any());
            verify(paymentRefundRepository, atLeastOnce()).save(argThat(refund ->
                    refund.getStatus() == PaymentRefund.RefundStatus.FAILED));
        }
    }

    @Nested
    @DisplayName("Query Tests")
    class QueryTests {

        @Test
        @DisplayName("Should get payments by booking ID")
        void testGetPaymentsByBooking() {
            when(paymentRepository.findByBookingIdOrderByCreatedAtDesc(100))
                    .thenReturn(List.of(testPayment));

            var payments = paymentService.getPaymentsByBooking(100);

            assertEquals(1, payments.size());
            assertEquals(testPayment.getId(), payments.get(0).getId());
        }

        @Test
        @DisplayName("Should get payments by status")
        void testGetPaymentsByStatus() {
            when(paymentRepository.findByStatusOrderByCreatedAtDesc(Payment.PaymentStatus.PAID))
                    .thenReturn(List.of(testPayment));

            var payments = paymentService.getPaymentsByStatus(Payment.PaymentStatus.PAID);

            assertEquals(1, payments.size());
        }
    }
}
