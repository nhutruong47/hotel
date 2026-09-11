package com.hsf.hotel.payment.api;

import com.hsf.hotel.config.ApiResponse;
import com.hsf.hotel.config.ErrorCodes;
import com.hsf.hotel.exception.ApiException;
import com.hsf.hotel.booking.model.Booking;
import com.hsf.hotel.booking.model.BookingStatus;
import com.hsf.hotel.booking.repository.BookingRepository;
import com.hsf.hotel.booking.service.BookingService;
import com.hsf.hotel.payment.model.Payment;
import com.hsf.hotel.payment.repository.PaymentRepository;
import com.hsf.hotel.payment.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@com.hsf.hotel.config.ApiController
@RequestMapping(com.hsf.hotel.config.ApiPaths.V1 + "/payments/webhook")
public class SepayWebhookApi {

    private static final Logger log = LoggerFactory.getLogger(SepayWebhookApi.class);
    private static final Pattern BOOKING_CODE_PATTERN = Pattern.compile("(?i)NV[-_\\s]?(\\d+)");

    private final BookingRepository bookingRepository;
    private final BookingService bookingService;
    private final PaymentRepository paymentRepository;
    private final String configuredApiKey;

    public SepayWebhookApi(BookingRepository bookingRepository,
                           BookingService bookingService,
                           PaymentRepository paymentRepository,
                           @Value("${SEPAY_API_KEY:}") String configuredApiKey) {
        this.bookingRepository = bookingRepository;
        this.bookingService = bookingService;
        this.paymentRepository = paymentRepository;
        this.configuredApiKey = configuredApiKey;
    }

    public static class SepayTransactionPayload {
        public Long id;
        public String gateway;
        public String transactionDate;
        public String accountNumber;
        public String content;
        public String transferType;
        public BigDecimal transferAmount;
        public String referenceCode;
        public String description;
    }

    @PostMapping("/sepay")
    @Transactional
    public ResponseEntity<ApiResponse<?>> handleSepayWebhook(
            @RequestBody SepayTransactionPayload payload,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        log.info("Received SePay webhook: id={}, amount={}, content={}",
                payload.id, payload.transferAmount, payload.content);

        // 1. SePay bypasses the generic HMAC filter, so its own API key must
        // be configured and compared exactly. Never accept a substring match.
        if (configuredApiKey == null || configuredApiKey.isBlank()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "SEPAY_NOT_CONFIGURED",
                    "SePay webhook authentication is not configured");
        }
        String suppliedApiKey = extractApiKey(authHeader);
        if (!constantTimeEquals(configuredApiKey, suppliedApiKey)) {
            log.warn("SePay webhook unauthorized attempt from IP {}", request.getRemoteAddr());
            throw new ApiException(HttpStatus.UNAUTHORIZED, ErrorCodes.UNAUTHORIZED, "Invalid SePay API key");
        }

        // 2. Only process incoming money (transferType == "in")
        if (payload.transferType != null && !"in".equalsIgnoreCase(payload.transferType)) {
            log.info("Ignoring non-inbound SePay transfer (type={})", payload.transferType);
            return ResponseEntity.ok(ApiResponse.ok(Map.of("success", true, "ignored", true)));
        }

        // 3. Extract Booking ID from transfer content (e.g. "NV-12", "NV12", "NV_12")
        String content = payload.content != null ? payload.content : payload.description;
        if (content == null || content.isBlank()) {
            log.warn("No content in SePay transaction #{}", payload.id);
            return ResponseEntity.ok(ApiResponse.ok(Map.of("success", false, "reason", "No content found")));
        }

        Matcher matcher = BOOKING_CODE_PATTERN.matcher(content);
        if (!matcher.find()) {
            log.warn("Could not find Booking reference pattern 'NV-xxx' in content: {}", content);
            return ResponseEntity.ok(ApiResponse.ok(Map.of("success", false, "reason", "Booking reference not matched")));
        }

        Integer bookingId;
        try {
            bookingId = Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException e) {
            log.error("Failed to parse booking ID from matcher group: {}", matcher.group(1));
            return ResponseEntity.ok(ApiResponse.ok(Map.of("success", false, "reason", "Invalid booking ID format")));
        }

        Optional<Booking> optBooking = bookingRepository.findByIdForUpdate(bookingId);
        if (optBooking.isEmpty()) {
            log.warn("Booking #{} referenced in SePay transaction #{} does not exist", bookingId, payload.id);
            return ResponseEntity.ok(ApiResponse.ok(Map.of("success", false, "reason", "Booking not found")));
        }

        Booking booking = optBooking.get();

        // 4. Verify transferred amount before creating or mutating any payment
        // record. Partial/over-payments require manual reconciliation.
        if (payload.transferAmount == null || payload.transferAmount.signum() <= 0) {
            log.warn("Invalid transfer amount in SePay transaction #{}", payload.id);
            return ResponseEntity.ok(ApiResponse.ok(Map.of("success", false, "reason", "Invalid amount")));
        }
        BigDecimal expectedAmount = booking.getTotalPrice() != null
                ? booking.getTotalPrice() : BigDecimal.ZERO;
        if (expectedAmount.compareTo(payload.transferAmount) != 0) {
            log.warn("SePay amount mismatch for booking #{}: expected={}, received={}",
                    bookingId, expectedAmount, payload.transferAmount);
            return ResponseEntity.ok(ApiResponse.ok(Map.of(
                    "success", false,
                    "reason", "Payment amount mismatch",
                    "requiresReview", true)));
        }

        // 5. Idempotency check by immutable SePay transaction identity.
        String transactionRef;
        if (payload.referenceCode != null && !payload.referenceCode.isBlank()) {
            transactionRef = payload.referenceCode.trim();
        } else if (payload.id != null) {
            transactionRef = "SEPAY-" + payload.id;
        } else {
            return ResponseEntity.ok(ApiResponse.ok(Map.of(
                    "success", false, "reason", "Missing transaction identity")));
        }

        Optional<Payment> existingPayment = paymentRepository.findByTransactionRef(transactionRef);
        if (existingPayment.isPresent()) {
            Payment existing = existingPayment.get();
            if (!existing.getBooking().getId().equals(bookingId)) {
                throw new ApiException(HttpStatus.CONFLICT, ErrorCodes.CONFLICT,
                        "Transaction reference belongs to another booking");
            }
            if (existing.getAmount().compareTo(payload.transferAmount) != 0) {
                throw new ApiException(HttpStatus.CONFLICT, ErrorCodes.CONFLICT,
                        "Transaction reference was reused with a different amount");
            }
            if (existing.getStatus() == Payment.PaymentStatus.PAID
                    || existing.getStatus() == Payment.PaymentStatus.PARTIALLY_REFUNDED
                    || existing.getStatus() == Payment.PaymentStatus.REFUNDED) {
                log.info("SePay transaction {} already processed for booking #{}", transactionRef, bookingId);
                return ResponseEntity.ok(ApiResponse.ok(Map.of("success", true, "idempotent", true)));
            }
        }

        // 6. Record Payment and confirm only an actively held booking.
        Payment payment = existingPayment.orElseGet(() -> {
            Payment p = new Payment(booking, payload.transferAmount, Payment.PaymentMethod.BANK_TRANSFER,
                    Payment.PaymentStatus.PAID, "PAY-" + System.currentTimeMillis());
            p.setGateway(Payment.PaymentGateway.SEPAY);
            p.setCurrency("VND");
            p.setCreatedAt(LocalDateTime.now());
            return p;
        });

        payment.setStatus(Payment.PaymentStatus.PAID);
        payment.setTransactionRef(transactionRef);
        payment.setCompletedAt(LocalDateTime.now());
        payment.setNotes("SePay auto reconciliation: Gateway " + payload.gateway + ", Account " + payload.accountNumber);
        paymentRepository.save(payment);

        // 7. Transition Booking to PAID. A late/duplicate transfer is still
        // recorded because money was received, but it cannot revive inventory.
        boolean requiresReview = booking.getStatus() != BookingStatus.PENDING_PAYMENT;
        if (booking.getStatus() == BookingStatus.PENDING_PAYMENT) {
            bookingService.confirmPayment(booking.getId(), transactionRef, payload.transferAmount);
            log.info("Successfully confirmed booking #{} via automated SePay webhook", bookingId);
        } else {
            payment.setNotes(payment.getNotes() + "; late payment for booking status " + booking.getStatus()
                    + " - manual review/refund required");
            paymentRepository.save(payment);
            log.warn("Recorded late SePay payment {} for booking #{} in status {}",
                    transactionRef, bookingId, booking.getStatus());
        }

        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "success", true,
                "bookingId", bookingId,
                "transactionRef", transactionRef,
                "amount", payload.transferAmount,
                "requiresReview", requiresReview
        )));
    }

    private static String extractApiKey(String authorization) {
        if (authorization == null) return "";
        return authorization.replaceFirst("(?i)^\\s*(Apikey|Bearer)\\s+", "").trim();
    }

    private static boolean constantTimeEquals(String expected, String supplied) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                supplied.getBytes(StandardCharsets.UTF_8));
    }
}
