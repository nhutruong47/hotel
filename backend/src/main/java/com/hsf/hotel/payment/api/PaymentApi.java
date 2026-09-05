package com.hsf.hotel.payment.api;
import com.hsf.hotel.room.model.Room;

import com.hsf.hotel.config.ApiResponse;
import com.hsf.hotel.exception.ApiException;
import com.hsf.hotel.exception.BusinessRuleException;
import com.hsf.hotel.exception.ResourceNotFoundException;
import com.hsf.hotel.booking.model.Booking;
import com.hsf.hotel.payment.model.Payment;
import com.hsf.hotel.user.model.User;
import com.hsf.hotel.booking.repository.BookingRepository;
import com.hsf.hotel.booking.service.BookingService;
import com.hsf.hotel.payment.service.PaymentService;
import com.hsf.hotel.service.payment.StripePaymentAdapter;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Payment API with Stripe integration.
 * 
 * <p>Supports:
 * <ul>
 *   <li>Manual payment recording (bank transfer, cash)</li>
 *   <li>Stripe Checkout Session for card payments</li>
 *   <li>Webhook handling for payment confirmations</li>
 *   <li>Refund processing</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentApi {

    private static final Logger log = LoggerFactory.getLogger(PaymentApi.class);

    private final PaymentService paymentService;
    private final BookingService bookingService;
    private final BookingRepository bookingRepository;
    private final StripePaymentAdapter stripeAdapter;
    private final String baseUrl;

    public PaymentApi(
            PaymentService paymentService,
            BookingService bookingService,
            BookingRepository bookingRepository,
            StripePaymentAdapter stripeAdapter,
            @Value("${app.base-url:http://localhost:5173}") String baseUrl) {
        this.paymentService = paymentService;
        this.bookingService = bookingService;
        this.bookingRepository = bookingRepository;
        this.stripeAdapter = stripeAdapter;
        this.baseUrl = baseUrl;
    }

    // ============== Request DTOs ==============

    public static class CreatePaymentRequest {
        @NotNull(message = "Thiếu bookingId")
        public Integer bookingId;

        @NotNull(message = "Thiếu số tiền")
        @Positive(message = "Số tiền phải lớn hơn 0")
        public BigDecimal amount;

        @Size(max = 32, message = "Phương thức thanh toán không hợp lệ")
        public String method;

        @Size(max = 100, message = "Mã giao dịch quá dài")
        public String transactionRef;

        public String notes;
    }

    public static class StripeCheckoutRequest {
        @NotNull(message = "Thiếu bookingId")
        public Integer bookingId;

        public String successUrl;

        public String cancelUrl;
    }

    public static class RefundRequest {
        @NotNull(message = "Thiếu số tiền hoàn")
        @Positive(message = "Số tiền hoàn phải lớn hơn 0")
        public BigDecimal amount;

        public String reason;
    }

    // ============== Auth Helpers ==============

    private User requireUser(HttpSession session) {
        Object u = session.getAttribute("user");
        if (!(u instanceof User user)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, com.hsf.hotel.config.ErrorCodes.UNAUTHORIZED, "Chưa đăng nhập");
        }
        return user;
    }

    private void requireAdmin(User user) {
        if (!"ADMIN".equals(user.getRole())) {
            throw new ApiException(HttpStatus.FORBIDDEN, com.hsf.hotel.config.ErrorCodes.FORBIDDEN, "Chỉ admin mới có quyền");
        }
    }

    private Booking getBookingOrThrow(Integer bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
    }

    private void checkBookingAccess(User user, Booking booking) {
        if (!"ADMIN".equals(user.getRole()) && !booking.getUser().getId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, com.hsf.hotel.config.ErrorCodes.FORBIDDEN,
                    "Bạn không có quyền thực hiện thao tác này");
        }
    }

    // ============== Manual Payment Recording ==============

    /**
     * Create a manual payment record (for bank transfer, cash, etc.)
     */
    @PostMapping
    public ResponseEntity<ApiResponse> create(@Valid @RequestBody CreatePaymentRequest req,
                                              HttpSession session) {
        User user = requireUser(session);
        Booking booking = getBookingOrThrow(req.bookingId);
        checkBookingAccess(user, booking);

        Payment.PaymentMethod method = parsePaymentMethod(req.method);

        Payment payment = paymentService.createPayment(booking, req.amount, method,
                req.transactionRef, req.notes);

        Map<String, Object> data = new HashMap<>();
        data.put("payment", payment);
        data.put("message", "Đã ghi nhận thanh toán. Vui lòng chờ admin xác nhận.");
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    // ============== Stripe Checkout Session ==============

    /**
     * Create a Stripe Checkout Session for card payment.
     * Returns the session ID and checkout URL.
     */
    @PostMapping("/stripe-checkout")
    public ResponseEntity<ApiResponse> createStripeCheckout(
            @Valid @RequestBody StripeCheckoutRequest req,
            HttpSession session) {
        User user = requireUser(session);
        Booking booking = getBookingOrThrow(req.bookingId);
        checkBookingAccess(user, booking);

        // Validate booking is in correct state
        if (booking.getStatus() != com.hsf.hotel.booking.model.BookingStatus.PENDING_PAYMENT) {
            throw new BusinessRuleException("INVALID_STATE",
                    "Booking không ở trạng thái chờ thanh toán");
        }

        // Check if Stripe is configured
        if (!stripeAdapter.isRealStripeEnabled()) {
            // Return mock response for development
            log.warn("Stripe not configured - returning mock checkout session");
            return ResponseEntity.ok(ApiResponse.ok(Map.of(
                    "mode", "mock",
                    "message", "Stripe not configured. Use mock payment for testing.",
                    "bookingId", booking.getId(),
                    "amount", booking.getTotalPrice(),
                    "currency", "VND"
            )));
        }

        try {
            // Build Stripe checkout session
            SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(buildUrl(req.successUrl, booking.getId()))
                    .setCancelUrl(buildUrl(req.cancelUrl, booking.getId()))
                    .putMetadata("booking_id", String.valueOf(booking.getId()))
                    .putMetadata("booking_reference", "NV-" + booking.getId())
                    .putMetadata("user_id", String.valueOf(user.getId()));

            // Add line items
            long amountInSmallestUnit = booking.getTotalPrice().longValue(); // VND
            String description = String.format("Booking #%d - %s (%s to %s)",
                    booking.getId(),
                    booking.getRoom() != null ? booking.getRoom().getRoomNumber() : "Room",
                    booking.getCheckInDate(),
                    booking.getCheckOutDate());

            paramsBuilder.addLineItem(SessionCreateParams.LineItem.builder()
                    .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency("vnd")
                            .setUnitAmount(amountInSmallestUnit)
                            .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                    .setName("Villa Booking - " + booking.getRoom().getRoomNumber())
                                    .setDescription(description)
                                    .build())
                            .build())
                    .setQuantity(1L)
                    .build());

            Session stripeSession = Session.create(paramsBuilder.build());

            // Create payment intent in our system
            Payment payment = paymentService.createPaymentIntent(booking, Payment.PaymentMethod.CARD);

            log.info("Created Stripe Checkout Session: {} for booking #{}", 
                    stripeSession.getId(), booking.getId());

            return ResponseEntity.ok(ApiResponse.ok(Map.of(
                    "sessionId", stripeSession.getId(),
                    "checkoutUrl", stripeSession.getUrl(),
                    "paymentIntentId", payment.getIntentId(),
                    "amount", booking.getTotalPrice(),
                    "currency", "VND"
            )));

        } catch (StripeException e) {
            log.error("Failed to create Stripe checkout session: {}", e.getMessage());
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "STRIPE_ERROR",
                    "Không thể tạo phiên thanh toán: " + e.getMessage());
        }
    }

    /**
     * Get Stripe checkout session status.
     */
    @GetMapping("/stripe-session/{sessionId}")
    public ResponseEntity<ApiResponse> getStripeSession(
            @PathVariable String sessionId,
            HttpSession session) {
        User user = requireUser(session);

        if (!stripeAdapter.isRealStripeEnabled()) {
            return ResponseEntity.ok(ApiResponse.ok(Map.of(
                    "mode", "mock",
                    "sessionId", sessionId
            )));
        }

        try {
            Session sessionObj = Session.retrieve(sessionId);
            Map<String, String> metadata = sessionObj.getMetadata();
            String metadataUserId = metadata != null ? metadata.get("user_id") : null;
            String metadataBookingId = metadata != null ? metadata.get("booking_id") : null;
            if (!"ADMIN".equals(user.getRole())) {
                if (metadataUserId != null && !metadataUserId.equals(String.valueOf(user.getId()))) {
                    throw new ApiException(HttpStatus.FORBIDDEN, com.hsf.hotel.config.ErrorCodes.FORBIDDEN,
                            "Báº¡n khÃ´ng cÃ³ quyá»n xem phiÃªn thanh toÃ¡n nÃ y");
                }
                if (metadataUserId == null && metadataBookingId != null) {
                    Integer bookingId;
                    try {
                        bookingId = Integer.parseInt(metadataBookingId);
                    } catch (NumberFormatException ex) {
                        throw new ApiException(HttpStatus.FORBIDDEN, com.hsf.hotel.config.ErrorCodes.FORBIDDEN,
                                "PhiÃªn thanh toÃ¡n khÃ´ng há»£p lá»‡");
                    }
                    Booking booking = getBookingOrThrow(bookingId);
                    checkBookingAccess(user, booking);
                }
            }
            
            Map<String, Object> data = new HashMap<>();
            data.put("status", sessionObj.getStatus());
            data.put("paymentStatus", sessionObj.getPaymentStatus());
            data.put("amountTotal", sessionObj.getAmountTotal());
            data.put("currency", sessionObj.getCurrency());
            data.put("bookingId", metadataBookingId);
            return ResponseEntity.ok(ApiResponse.ok(data));
        } catch (StripeException e) {
            log.error("Failed to retrieve Stripe session: {}", e.getMessage());
            throw new ApiException(HttpStatus.NOT_FOUND, "SESSION_NOT_FOUND",
                    "Không tìm thấy phiên thanh toán");
        }
    }

    // ============== Payment Intent ==============

    /**
     * Create a payment intent (for custom Stripe integration).
     */
    @PostMapping("/intent")
    public ResponseEntity<ApiResponse> createIntent(
            @Valid @RequestBody CreatePaymentRequest req,
            HttpSession session) {
        User user = requireUser(session);
        Booking booking = getBookingOrThrow(req.bookingId);
        checkBookingAccess(user, booking);

        Payment.PaymentMethod method = parsePaymentMethod(req.method);
        PaymentService.PaymentIntentResult result = paymentService.createPaymentIntentWithClientSecret(booking, method);
        Payment payment = result.payment();

        Map<String, Object> data = new HashMap<>();
        data.put("intentId", payment.getIntentId());
        data.put("amount", payment.getAmount());
        data.put("currency", payment.getCurrency());

        if (result.clientSecret() != null && !result.clientSecret().isBlank()) {
            data.put("clientSecret", result.clientSecret());
        }

        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    // ============== Webhook Handler ==============

    /**
     * Handle Stripe webhooks for payment confirmation.
     * This endpoint should be called by Stripe after payment completion.
     */
    @PostMapping("/webhook/stripe")
    public ResponseEntity<ApiResponse> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String signature,
            HttpServletRequest request) {
        
        log.debug("Received Stripe webhook");

        try {
            if (signature == null || signature.isBlank()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "WEBHOOK_SIGNATURE_MISSING",
                        "Missing Stripe-Signature header");
            }
            com.stripe.model.Event event = stripeAdapter.verifyWebhookSignature(payload, signature);

            // Handle the event
            switch (event.getType()) {
                case "checkout.session.completed":
                    handleCheckoutCompleted(event);
                    break;
                    
                case "payment_intent.succeeded":
                    handlePaymentIntentSucceeded(event);
                    break;
                    
                case "payment_intent.payment_failed":
                    handlePaymentIntentFailed(event);
                    break;
                    
                case "charge.refunded":
                    handleChargeRefunded(event);
                    break;
                    
                default:
                    log.debug("Unhandled webhook event type: {}", event.getType());
            }

            return ResponseEntity.ok(ApiResponse.ok(Map.of("received", true)));

        } catch (Exception e) {
            log.error("Error processing webhook: {}", e.getMessage(), e);
            throw new ApiException(HttpStatus.BAD_REQUEST, "WEBHOOK_ERROR",
                    "Lỗi xử lý webhook: " + e.getMessage());
        }
    }

    /**
     * Handle generic gateway webhooks (legacy).
     */
    @PostMapping("/webhook/{gateway}")
    public ResponseEntity<ApiResponse> handleGenericWebhook(
            @PathVariable String gateway,
            @RequestBody Map<String, Object> payload) {
        
        String intentId = (String) payload.get("intentId");
        String status = (String) payload.get("status");
        
        Payment payment = paymentService.handleWebhook(intentId, status, payload.toString());
        
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "status", "received",
                "paymentId", payment.getId()
        )));
    }

    // ============== Admin Operations ==============

    /**
     * Mark payment as complete (manual confirmation).
     */
    @PostMapping("/{id}/complete")
    public ResponseEntity<ApiResponse> markComplete(
            @PathVariable Integer id,
            HttpSession session) {
        User user = requireUser(session);
        requireAdmin(user);
        
        Payment payment = paymentService.markCompleted(id, user);
        bookingService.confirmPayment(
                payment.getBooking().getId(),
                payment.getTransactionRef(),
                payment.getAmount());
        
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "payment", payment,
                "message", "Đã xác nhận thanh toán thành công"
        )));
    }

    /**
     * Mark payment as failed.
     */
    @PostMapping("/{id}/fail")
    public ResponseEntity<ApiResponse> markFailed(
            @PathVariable Integer id,
            @RequestBody(required = false) Map<String, String> body,
            HttpSession session) {
        User user = requireUser(session);
        requireAdmin(user);
        
        String reason = body != null ? body.get("reason") : null;
        Payment payment = paymentService.markFailed(id, reason);
        
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "payment", payment,
                "message", "Đã đánh dấu thanh toán thất bại"
        )));
    }

    /**
     * Process refund.
     */
    @PostMapping("/{id}/refund")
    public ResponseEntity<ApiResponse> refund(
            @PathVariable Integer id,
            @Valid @RequestBody RefundRequest req,
            HttpSession session) {
        User user = requireUser(session);
        requireAdmin(user);
        
        try {
            Payment payment = paymentService.refund(id, req.amount, req.reason, user);
            return ResponseEntity.ok(ApiResponse.ok(Map.of(
                    "payment", payment,
                    "message", "Đã hoàn tiền " + req.amount + " VNĐ"
            )));
        } catch (BusinessRuleException e) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, e.getCode(), e.getMessage());
        }
    }

    // ============== Query Endpoints ==============

    /**
     * Get payments for a booking.
     */
    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<ApiResponse> forBooking(
            @PathVariable Integer bookingId,
            HttpSession session) {
        User user = requireUser(session);
        Booking booking = getBookingOrThrow(bookingId);
        checkBookingAccess(user, booking);
        
        List<Payment> payments = paymentService.getPaymentsByBooking(bookingId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("payments", payments)));
    }

    /**
     * Get all payments (admin only).
     */
    @GetMapping
    public ResponseEntity<ApiResponse> all(HttpSession session) {
        User user = requireUser(session);
        requireAdmin(user);
        
        List<Payment> payments = paymentService.getPaymentsByStatus(Payment.PaymentStatus.PAID);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("payments", payments)));
    }

    // ============== Helper Methods ==============

    private Payment.PaymentMethod parsePaymentMethod(String method) {
        if (method == null || method.isBlank()) {
            return Payment.PaymentMethod.BANK_TRANSFER;
        }
        try {
            return Payment.PaymentMethod.valueOf(method.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_METHOD",
                    "Phương thức thanh toán không hợp lệ: " + method);
        }
    }

    private String buildUrl(String url, Integer bookingId) {
        String safeUrl = normalizeReturnUrl(url);
        if (safeUrl != null && !safeUrl.isBlank()) {
            // Append booking ID if not already in URL
            if (!safeUrl.contains("bookingId=")) {
                String separator = safeUrl.contains("?") ? "&" : "?";
                return safeUrl + separator + "bookingId=" + bookingId;
            }
            return safeUrl;
        }
        // Default success/cancel URLs
        return baseUrl + "/booking/success?bookingId=" + bookingId;
    }

    private String normalizeReturnUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            URI base = URI.create(baseUrl);
            URI candidate = URI.create(url.trim());
            if (!candidate.isAbsolute()) {
                String path = candidate.toString().startsWith("/") ? candidate.toString() : "/" + candidate;
                return base.resolve(path).toString();
            }
            boolean sameScheme = base.getScheme() != null && base.getScheme().equalsIgnoreCase(candidate.getScheme());
            boolean sameHost = base.getHost() != null && base.getHost().equalsIgnoreCase(candidate.getHost());
            int basePort = base.getPort() == -1 ? defaultPort(base.getScheme()) : base.getPort();
            int candidatePort = candidate.getPort() == -1 ? defaultPort(candidate.getScheme()) : candidate.getPort();
            if (sameScheme && sameHost && basePort == candidatePort) {
                return candidate.toString();
            }
        } catch (IllegalArgumentException ignored) {
            // Fall through to default URL.
        }
        log.warn("Rejected untrusted payment return URL");
        return null;
    }

    private static int defaultPort(String scheme) {
        if ("https".equalsIgnoreCase(scheme)) return 443;
        if ("http".equalsIgnoreCase(scheme)) return 80;
        return -1;
    }

    // ============== Webhook Event Handlers ==============

    private void handleCheckoutCompleted(com.stripe.model.Event event) {
        try {
            Session session = (Session) event.getDataObjectDeserializer()
                    .getObject().orElse(null);
            
            if (session == null) {
                log.error("Failed to deserialize checkout session");
                return;
            }

            String bookingIdStr = session.getMetadata().get("booking_id");
            if (bookingIdStr == null) {
                log.error("No booking_id in session metadata");
                return;
            }

            Integer bookingId = Integer.parseInt(bookingIdStr);
            log.info("Checkout completed for booking #{}", bookingId);

            // Update payment status
            String paymentIntentId = session.getPaymentIntent();
            if (paymentIntentId != null) {
                paymentService.handleWebhook(paymentIntentId, "succeeded",
                        "{\"type\": \"checkout.session.completed\", \"session_id\": \"" + session.getId() + "\"}");
            }

        } catch (Exception e) {
            log.error("Error handling checkout.session.completed: {}", e.getMessage(), e);
        }
    }

    private void handlePaymentIntentSucceeded(com.stripe.model.Event event) {
        try {
            com.stripe.model.PaymentIntent intent = (com.stripe.model.PaymentIntent) event.getDataObjectDeserializer()
                    .getObject().orElse(null);
            
            if (intent == null) return;

            log.info("PaymentIntent succeeded: {}", intent.getId());
            paymentService.handleWebhook(intent.getId(), "succeeded",
                    "{\"type\": \"payment_intent.succeeded\"}");

        } catch (Exception e) {
            log.error("Error handling payment_intent.succeeded: {}", e.getMessage(), e);
        }
    }

    private void handlePaymentIntentFailed(com.stripe.model.Event event) {
        try {
            com.stripe.model.PaymentIntent intent = (com.stripe.model.PaymentIntent) event.getDataObjectDeserializer()
                    .getObject().orElse(null);
            
            if (intent == null) return;

            log.warn("PaymentIntent failed: {}", intent.getId());
            String errorMsg = intent.getLastPaymentError() != null 
                    ? intent.getLastPaymentError().getMessage() 
                    : "Payment failed";
            
            paymentService.handleWebhook(intent.getId(), "failed",
                    "{\"type\": \"payment_intent.payment_failed\", \"error\": \"" + errorMsg + "\"}");

        } catch (Exception e) {
            log.error("Error handling payment_intent.payment_failed: {}", e.getMessage(), e);
        }
    }

    private void handleChargeRefunded(com.stripe.model.Event event) {
        try {
            com.stripe.model.Charge charge = (com.stripe.model.Charge) event.getDataObjectDeserializer()
                    .getObject().orElse(null);
            
            if (charge == null) return;

            log.info("Charge refunded: {} amount={}", charge.getId(), charge.getAmountRefunded());
            // Refund handling is done through the refund API

        } catch (Exception e) {
            log.error("Error handling charge.refunded: {}", e.getMessage(), e);
        }
    }
}
