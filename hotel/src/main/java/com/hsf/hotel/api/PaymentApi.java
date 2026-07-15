package com.hsf.hotel.api;

import com.hsf.hotel.config.ApiResponse;
import com.hsf.hotel.exception.ApiException;
import com.hsf.hotel.exception.BusinessRuleException;
import com.hsf.hotel.exception.ResourceNotFoundException;
import com.hsf.hotel.model.Booking;
import com.hsf.hotel.model.Payment;
import com.hsf.hotel.model.User;
import com.hsf.hotel.repository.BookingRepository;
import com.hsf.hotel.service.BookingService;
import com.hsf.hotel.service.PaymentService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentApi {

    private final PaymentService paymentService;
    private final BookingService bookingService;
    private final BookingRepository bookingRepository;

    public PaymentApi(PaymentService paymentService,
                      BookingService bookingService,
                      BookingRepository bookingRepository) {
        this.paymentService = paymentService;
        this.bookingService = bookingService;
        this.bookingRepository = bookingRepository;
    }

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

    public static class RefundRequest {
        @NotNull(message = "Thiếu số tiền hoàn")
        @Positive(message = "Số tiền hoàn phải lớn hơn 0")
        public BigDecimal amount;

        public String reason;
    }

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

    @PostMapping
    public ResponseEntity<ApiResponse> create(@Valid @RequestBody CreatePaymentRequest req,
                                              HttpSession session) {
        User user = requireUser(session);
        Booking booking = bookingRepository.findById(req.bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", req.bookingId));

        // Customer can only create payment for their own booking; admin can create for any.
        if (!"ADMIN".equals(user.getRole()) && !booking.getUser().getId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, com.hsf.hotel.config.ErrorCodes.FORBIDDEN,
                    "Bạn không có quyền thanh toán cho đơn này");
        }

        Payment.PaymentMethod method;
        try {
            method = req.method == null || req.method.isBlank()
                    ? Payment.PaymentMethod.BANK_TRANSFER
                    : Payment.PaymentMethod.valueOf(req.method.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, com.hsf.hotel.config.ErrorCodes.BAD_REQUEST,
                    "Phương thức thanh toán không hợp lệ: " + req.method);
        }

        Payment payment = paymentService.createPayment(booking, req.amount, method,
                req.transactionRef, req.notes);

        Map<String, Object> data = new HashMap<>();
        data.put("payment", payment);
        data.put("message", "Đã ghi nhận thanh toán. Vui lòng chờ admin xác nhận.");
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @PostMapping("/intent")
    public ResponseEntity<ApiResponse> createIntent(@Valid @RequestBody CreatePaymentRequest req, HttpSession session) {
        User user = requireUser(session);
        Booking booking = bookingRepository.findById(req.bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", req.bookingId));

        if (!"ADMIN".equals(user.getRole()) && !booking.getUser().getId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, com.hsf.hotel.config.ErrorCodes.FORBIDDEN,
                    "Bạn không có quyền thanh toán cho đơn này");
        }

        Payment.PaymentMethod method;
        try {
            method = req.method == null || req.method.isBlank()
                    ? Payment.PaymentMethod.CARD
                    : Payment.PaymentMethod.valueOf(req.method.toUpperCase());
        } catch (IllegalArgumentException ex) {
            method = Payment.PaymentMethod.OTHER;
        }

        Payment payment = paymentService.createPaymentIntent(booking, method);

        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "intentId", payment.getIntentId(),
                "amount", payment.getAmount(),
                "currency", payment.getCurrency()
        )));
    }

    @PostMapping("/webhook/{gateway}")
    public ResponseEntity<ApiResponse> handleWebhook(@PathVariable String gateway, @RequestBody Map<String, Object> payload) {
        String intentId = (String) payload.get("intentId");
        String status = (String) payload.get("status");
        
        Payment payment = paymentService.handleWebhook(intentId, status, payload.toString());
        return ResponseEntity.ok(ApiResponse.ok(Map.of("status", "received", "paymentId", payment.getId())));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<ApiResponse> markComplete(@PathVariable Integer id, HttpSession session) {
        User user = requireUser(session);
        requireAdmin(user);
        Payment payment = paymentService.markCompleted(id, user);
        // Also confirm booking payment.
        bookingService.confirmPayment(payment.getBooking().getId(),
                payment.getTransactionRef(), payment.getAmount());
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "payment", payment,
                "message", "Đã xác nhận thanh toán thành công"
        )));
    }

    @PostMapping("/{id}/fail")
    public ResponseEntity<ApiResponse> markFailed(@PathVariable Integer id,
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

    @PostMapping("/{id}/refund")
    public ResponseEntity<ApiResponse> refund(@PathVariable Integer id,
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

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<ApiResponse> forBooking(@PathVariable Integer bookingId, HttpSession session) {
        User user = requireUser(session);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
        if (!"ADMIN".equals(user.getRole()) && !booking.getUser().getId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, com.hsf.hotel.config.ErrorCodes.FORBIDDEN,
                    "Bạn không có quyền xem thanh toán của đơn này");
        }
        List<Payment> payments = paymentService.getPaymentsByBooking(bookingId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("payments", payments)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse> all(HttpSession session) {
        User user = requireUser(session);
        requireAdmin(user);
        List<Payment> payments = paymentService.getPaymentsByStatus(Payment.PaymentStatus.PAID);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("payments", payments)));
    }
}