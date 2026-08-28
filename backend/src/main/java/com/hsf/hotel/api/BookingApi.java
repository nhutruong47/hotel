package com.hsf.hotel.api;

import com.hsf.hotel.config.ApiResponse;
import com.hsf.hotel.config.ErrorCodes;
import com.hsf.hotel.exception.ApiException;
import com.hsf.hotel.exception.ResourceNotFoundException;
import com.hsf.hotel.model.Booking;
import com.hsf.hotel.model.Room;
import com.hsf.hotel.model.User;
import com.hsf.hotel.security.AuditActions;
import com.hsf.hotel.service.AuditLogService;
import com.hsf.hotel.service.BookingService;
import com.hsf.hotel.service.RoomService;
import com.hsf.hotel.dto.BookingMapper;
import com.hsf.hotel.dto.BookingDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.hsf.hotel.service.VoucherService;
import java.time.temporal.ChronoUnit;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/bookings")
public class BookingApi {

    private final BookingService bookingService;
    private final RoomService roomService;
    private final com.hsf.hotel.service.InvoiceService invoiceService;
    private final com.hsf.hotel.repository.PaymentRepository paymentRepository;
    private final VoucherService voucherService;
    private final AuditLogService auditLogService;
    private final BookingMapper bookingMapper;

    public BookingApi(BookingService bookingService, RoomService roomService,
                      com.hsf.hotel.service.InvoiceService invoiceService,
                      com.hsf.hotel.repository.PaymentRepository paymentRepository,
                      VoucherService voucherService,
                      AuditLogService auditLogService,
                      BookingMapper bookingMapper) {
        this.bookingService = bookingService;
        this.roomService = roomService;
        this.invoiceService = invoiceService;
        this.paymentRepository = paymentRepository;
        this.voucherService = voucherService;
        this.auditLogService = auditLogService;
        this.bookingMapper = bookingMapper;
    }

    public static class CreateBookingRequest {

        @NotNull(message = "Thiếu roomId")
        public Integer roomId;

        @NotNull(message = "Thiếu ngày nhận phòng")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        public LocalDate checkIn;

        @NotNull(message = "Thiếu ngày trả phòng")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        public LocalDate checkOut;

        @NotBlank(message = "Thiếu tên khách")
        @Size(min = 2, max = 100, message = "Tên khách phải có từ 2 đến 100 ký tự")
        public String guestName;

        @Size(max = 32, message = "Số điện thoại không hợp lệ")
        public String guestPhone;

        @Email(message = "Email không hợp lệ")
        public String guestEmail;

        @Min(value = 1, message = "Số khách phải >= 1")
        public Integer guests;

        @Size(max = 1000, message = "Ghi chú tối đa 1000 ký tự")
        public String notes;

        @Size(max = 64, message = "Mã voucher quá dài")
        public String voucherCode;
    }

    public static class CancelBookingRequest {
        public String reason;
    }

    public static class PaymentConfirmationRequest {
        @Size(max = 100, message = "Mã giao dịch quá dài")
        public String transactionRef;

        @NotNull(message = "Thiếu số tiền thanh toán")
        public BigDecimal amount;

        public String paymentMethod;
    }

    @GetMapping
    public ResponseEntity<ApiResponse> myBookings(HttpSession session) {
        User user = currentUser(session);
        List<Booking> bookings = bookingService.getUserBookings(user);
        List<BookingDTO> dtos = bookingMapper.bookingsToBookingDTOs(bookings);
        return ResponseEntity.ok(ApiResponse.ok(dtos));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> get(@PathVariable Integer id, HttpSession session) {
        User user = currentUser(session);
        Booking b = bookingService.getBookingById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", id));
        if (!b.getUser().getId().equals(user.getId()) && !"ADMIN".equals(user.getRole())) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCodes.FORBIDDEN, "Không có quyền xem");
        }
        return ResponseEntity.ok(ApiResponse.ok(bookingMapper.bookingToBookingDTO(b)));
    }


    @PostMapping
    public ResponseEntity<ApiResponse> create(@Valid @RequestBody CreateBookingRequest req,
                                              HttpSession session,
                                              HttpServletRequest request) {
        User user = currentUser(session);

        Room room = roomService.getRoomById(req.roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", req.roomId));

        Booking booking = bookingService.createBooking(
                user, room, req.checkIn, req.checkOut,
                req.guestName, req.guestPhone, req.guestEmail,
                req.guests, req.notes, req.voucherCode);

        auditLogService.log(user, AuditActions.BOOKING_CREATE, "Booking", booking.getId(),
                "room=" + room.getId() + " checkIn=" + req.checkIn + " checkOut=" + req.checkOut,
                request);

        Map<String, Object> data = new HashMap<>();
        data.put("booking", bookingMapper.bookingToBookingDTO(booking));
        data.put("reference", "NV-" + booking.getId());
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse> cancel(@PathVariable Integer id,
                                              @RequestBody(required = false) CancelBookingRequest req,
                                              HttpSession session,
                                              HttpServletRequest request) {
        User user = currentUser(session);
        Booking booking = bookingService.cancelBooking(id, user, req != null ? req.reason : null);
        auditLogService.log(user, AuditActions.BOOKING_CANCEL, "Booking", id,
                "refund=" + booking.getRefundAmount() + " reason=" + (req != null ? req.reason : null),
                request);
        Map<String, Object> data = new HashMap<>();
        data.put("message", "Đã hủy đặt phòng thành công");
        data.put("refundPercentage", booking.getRefundPercentage());
        data.put("refundAmount", booking.getRefundAmount());
        data.put("booking", bookingMapper.bookingToBookingDTO(booking));
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @PostMapping("/{id}/payment")
    public ResponseEntity<ApiResponse> submitPayment(@PathVariable Integer id,
                                                     @Valid @RequestBody PaymentConfirmationRequest req,
                                                     HttpSession session,
                                                     HttpServletRequest request) {
        User user = currentUser(session);
        Booking booking = bookingService.getBookingById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", id));
        if (!booking.getUser().getId().equals(user.getId()) && !"ADMIN".equals(user.getRole())) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCodes.FORBIDDEN,
                    "Bạn không có quyền thanh toán cho đơn này");
        }
        Booking updated = bookingService.confirmPayment(id, req.transactionRef, req.amount);
        auditLogService.log(user, AuditActions.PAYMENT_CONFIRM, "Payment", updated.getId(),
                "amount=" + req.amount + " ref=" + req.transactionRef, request);
        Map<String, Object> data = new HashMap<>();
        data.put("message", "Thanh toán thành công. Đơn đặt phòng của bạn đã được xác nhận.");
        data.put("booking", bookingMapper.bookingToBookingDTO(updated));
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    /* ---------- booking modification ---------- */

    public static class ModifyBookingRequest {
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        public LocalDate checkIn;

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        public LocalDate checkOut;

        @Min(value = 1, message = "Số khách phải >= 1")
        public Integer guests;
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse> modify(@PathVariable Integer id,
                                               @Valid @RequestBody ModifyBookingRequest req,
                                               HttpSession session) {
        User user = currentUser(session);
        Booking booking = bookingService.modifyBooking(id, user, req.checkIn, req.checkOut, req.guests);
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "message", "Đã cập nhật thông tin đặt phòng",
                "booking", bookingMapper.bookingToBookingDTO(booking)
        )));
    }

    /* ---------- timeline ---------- */

    @GetMapping("/{id}/timeline")
    public ResponseEntity<ApiResponse> timeline(@PathVariable Integer id, HttpSession session) {
        User user = currentUser(session);
        Booking b = bookingService.getBookingById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", id));
        if (!b.getUser().getId().equals(user.getId()) && !"ADMIN".equals(user.getRole())) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCodes.FORBIDDEN, "Không có quyền xem");
        }
        return ResponseEntity.ok(ApiResponse.ok(bookingService.getBookingTimeline(id)));
    }

    /* ---------- invoice ---------- */
    
    @GetMapping("/{id}/invoice")
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable Integer id, HttpSession session) {
        User user = currentUser(session);
        Booking b = bookingService.getBookingById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", id));
        if (!b.getUser().getId().equals(user.getId()) && !"ADMIN".equals(user.getRole())) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCodes.FORBIDDEN, "Không có quyền tải hóa đơn");
        }
        
        List<com.hsf.hotel.model.Payment> payments = paymentRepository.findByBookingIdOrderByCreatedAtDesc(id);
        byte[] pdfBytes = invoiceService.generateInvoice(b, payments);
        
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "invoice-" + b.getId() + ".pdf");
        
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    /* ---------- pricing preview ---------- */

    @GetMapping("/pricing-preview")
    public ResponseEntity<ApiResponse> pricingPreview(
            @RequestParam Integer roomId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut) {
        Room room = roomService.getRoomById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", roomId));
        var pricing = bookingService.calculatePricing(room, checkIn, checkOut);
        Map<String, Object> data = new HashMap<>();
        data.put("subtotal", pricing.subtotal());
        data.put("serviceFee", pricing.serviceFee());
        data.put("taxAmount", pricing.taxAmount());
        data.put("total", pricing.total());
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/vouchers/validate")
    public ResponseEntity<ApiResponse> validateVoucher(
            @RequestParam String code,
            @RequestParam(required = false) Integer roomId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut) {
            
        VoucherService.VoucherValidationResult res = voucherService.validateVoucher(code);
        Map<String, Object> data = new HashMap<>();
        data.put("valid", res.valid);
        data.put("message", res.message);
        data.put("amount", res.amount != null ? res.amount.toString() : "0");
        data.put("percent", res.percent);
        
        if (res.voucher != null) {
            data.put("code", res.voucher.getCode());
            data.put("expiryDate", res.voucher.getExpiryDate() != null ? res.voucher.getExpiryDate().toString() : null);
        } else if (res.promotion != null) {
            com.hsf.hotel.model.Promotion p = res.promotion;
            data.put("code", p.getPromoCode());
            data.put("title", p.getTitle());
            data.put("minimumNights", p.getMinimumNights());
            data.put("minimumBookingAmount", p.getMinimumBookingAmount());
            
            if (roomId != null) {
                Room room = roomService.getRoomById(roomId).orElse(null);
                if (room != null && p.getRooms() != null && !p.getRooms().isEmpty() && !p.getRooms().contains(room)) {
                    data.put("valid", false);
                    data.put("message", "Mã ưu đãi này không áp dụng cho villa đã chọn");
                }
                
                if (checkIn != null && checkOut != null) {
                    long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
                    if (nights < p.getMinimumNights()) {
                        data.put("valid", false);
                        data.put("message", "Mã ưu đãi yêu cầu đặt tối thiểu " + p.getMinimumNights() + " đêm");
                    }
                    
                    if (room != null) {
                        BigDecimal subtotal = room.getPricePerNight().multiply(BigDecimal.valueOf(nights));
                        if (subtotal.compareTo(p.getMinimumBookingAmount()) < 0) {
                            data.put("valid", false);
                            data.put("message", "Mã ưu đãi yêu cầu giá trị đặt phòng tối thiểu từ " + p.getMinimumBookingAmount());
                        }
                    }
                }
            }
        }
        
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    private static User currentUser(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, ErrorCodes.UNAUTHORIZED, "Chưa đăng nhập");
        }
        return user;
    }
}
