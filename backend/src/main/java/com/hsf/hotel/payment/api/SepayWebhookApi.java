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
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
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
    public ResponseEntity<ApiResponse<?>> handleSepayWebhook(
            @RequestBody SepayTransactionPayload payload,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        log.info("Received SePay webhook: id={}, amount={}, content={}",
                payload.id, payload.transferAmount, payload.content);

        // 1. Verify Authorization if API Key is configured in .env
        if (configuredApiKey != null && !configuredApiKey.isBlank()) {
            boolean valid = false;
            if (authHeader != null) {
                String cleanHeader = authHeader.replace("Apikey", "").replace("Bearer", "").trim();
                if (configuredApiKey.equals(cleanHeader) || authHeader.contains(configuredApiKey)) {
                    valid = true;
                }
            }
            if (!valid) {
                log.warn("SePay webhook unauthorized attempt from IP {}", request.getRemoteAddr());
                throw new ApiException(HttpStatus.UNAUTHORIZED, ErrorCodes.UNAUTHORIZED, "Invalid SePay API key");
            }
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

        Optional<Booking> optBooking = bookingRepository.findById(bookingId);
        if (optBooking.isEmpty()) {
            log.warn("Booking #{} referenced in SePay transaction #{} does not exist", bookingId, payload.id);
            return ResponseEntity.ok(ApiResponse.ok(Map.of("success", false, "reason", "Booking not found")));
        }

        Booking booking = optBooking.get();

        // 4. Idempotency check by SePay transaction ID / reference code
        String transactionRef = payload.referenceCode != null && !payload.referenceCode.isBlank()
                ? payload.referenceCode
                : "SEPAY-" + payload.id;

        Optional<Payment> existingPayment = paymentRepository.findByTransactionRef(transactionRef);
        if (existingPayment.isPresent() && existingPayment.get().getStatus() == Payment.PaymentStatus.PAID) {
            log.info("SePay transaction {} already processed for booking #{}", transactionRef, bookingId);
            return ResponseEntity.ok(ApiResponse.ok(Map.of("success", true, "idempotent", true)));
        }

        // 5. Verify transferred amount against booking total price
        if (payload.transferAmount == null || payload.transferAmount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Invalid transfer amount in SePay transaction #{}", payload.id);
            return ResponseEntity.ok(ApiResponse.ok(Map.of("success", false, "reason", "Invalid amount")));
        }

        // 6. Record Payment & Confirm Booking
        Payment payment = existingPayment.orElseGet(() -> {
            Payment p = new Payment(booking, payload.transferAmount, Payment.PaymentMethod.BANK_TRANSFER,
                    Payment.PaymentStatus.PAID, "PAY-" + System.currentTimeMillis());
            p.setGateway(Payment.PaymentGateway.VIETQR);
            p.setCurrency("VND");
            p.setCreatedAt(LocalDateTime.now());
            return p;
        });

        payment.setStatus(Payment.PaymentStatus.PAID);
        payment.setTransactionRef(transactionRef);
        payment.setCompletedAt(LocalDateTime.now());
        payment.setNotes("SePay auto reconciliation: Gateway " + payload.gateway + ", Account " + payload.accountNumber);
        paymentRepository.save(payment);

        // 7. Transition Booking to PAID
        if (booking.getStatus() == BookingStatus.PENDING_PAYMENT || booking.getStatus() == BookingStatus.AWAITING_APPROVAL) {
            bookingService.confirmPayment(booking.getId(), transactionRef, payload.transferAmount);
            log.info("Successfully confirmed booking #{} via automated SePay webhook", bookingId);
        }

        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "success", true,
                "bookingId", bookingId,
                "transactionRef", transactionRef,
                "amount", payload.transferAmount
        )));
    }
}
