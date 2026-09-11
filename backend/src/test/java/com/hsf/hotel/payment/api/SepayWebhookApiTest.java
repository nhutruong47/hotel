package com.hsf.hotel.payment.api;

import com.hsf.hotel.booking.model.Booking;
import com.hsf.hotel.booking.model.BookingStatus;
import com.hsf.hotel.booking.repository.BookingRepository;
import com.hsf.hotel.booking.service.BookingService;
import com.hsf.hotel.exception.ApiException;
import com.hsf.hotel.payment.model.Payment;
import com.hsf.hotel.payment.repository.PaymentRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SepayWebhookApiTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private BookingService bookingService;
    @Mock private PaymentRepository paymentRepository;
    @Mock private HttpServletRequest request;

    private SepayWebhookApi api;
    private Booking booking;

    @BeforeEach
    void setUp() {
        api = new SepayWebhookApi(bookingRepository, bookingService, paymentRepository, "secret-key");
        booking = new Booking();
        booking.setId(42);
        booking.setStatus(BookingStatus.PENDING_PAYMENT);
        booking.setTotalPrice(BigDecimal.valueOf(1_000_000));
    }

    @Test
    void rejectsAuthorizationHeaderContainingButNotEqualToApiKey() {
        ApiException exception = assertThrows(ApiException.class, () ->
                api.handleSepayWebhook(payload(), "Bearer prefix-secret-key-suffix", request));

        assertEquals(401, exception.getStatus().value());
        verifyNoInteractions(bookingRepository, bookingService, paymentRepository);
    }

    @Test
    void rejectsAmountMismatchBeforeWritingPayment() {
        SepayWebhookApi.SepayTransactionPayload payload = payload();
        payload.transferAmount = BigDecimal.valueOf(999_999);
        when(bookingRepository.findByIdForUpdate(42)).thenReturn(Optional.of(booking));

        var response = api.handleSepayWebhook(payload, "Apikey secret-key", request);

        assertEquals(200, response.getStatusCode().value());
        verify(paymentRepository, never()).save(any());
        verify(bookingService, never()).confirmPayment(any(), any(), any());
    }

    @Test
    void confirmsAnExactlyMatchedIncomingTransferOnce() {
        when(bookingRepository.findByIdForUpdate(42)).thenReturn(Optional.of(booking));
        when(paymentRepository.findByTransactionRef("SEPAY-9001")).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bookingService.confirmPayment(42, "SEPAY-9001", BigDecimal.valueOf(1_000_000)))
                .thenReturn(booking);

        var response = api.handleSepayWebhook(payload(), "Bearer secret-key", request);

        assertEquals(200, response.getStatusCode().value());
        ArgumentCaptor<Payment> payment = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(payment.capture());
        assertEquals(Payment.PaymentGateway.SEPAY, payment.getValue().getGateway());
        assertEquals(Payment.PaymentStatus.PAID, payment.getValue().getStatus());
        verify(bookingService).confirmPayment(42, "SEPAY-9001", BigDecimal.valueOf(1_000_000));
    }

    @Test
    void recordsLateTransferWithoutRevivingExpiredBooking() {
        booking.setStatus(BookingStatus.EXPIRED);
        when(bookingRepository.findByIdForUpdate(42)).thenReturn(Optional.of(booking));
        when(paymentRepository.findByTransactionRef("SEPAY-9001")).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        api.handleSepayWebhook(payload(), "secret-key", request);

        verify(bookingService, never()).confirmPayment(any(), any(), any());
        ArgumentCaptor<Payment> payment = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, atLeastOnce()).save(payment.capture());
        assertTrue(payment.getValue().getNotes().contains("manual review/refund required"));
        assertEquals(BookingStatus.EXPIRED, booking.getStatus());
    }

    private SepayWebhookApi.SepayTransactionPayload payload() {
        SepayWebhookApi.SepayTransactionPayload payload = new SepayWebhookApi.SepayTransactionPayload();
        payload.id = 9001L;
        payload.transferType = "in";
        payload.transferAmount = BigDecimal.valueOf(1_000_000);
        payload.content = "Payment NV-42";
        payload.gateway = "SePay";
        return payload;
    }
}
