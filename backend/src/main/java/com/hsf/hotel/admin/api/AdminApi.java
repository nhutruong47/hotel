package com.hsf.hotel.admin.api;

import com.hsf.hotel.config.ApiResponse;
import com.hsf.hotel.config.ErrorCodes;
import com.hsf.hotel.exception.ApiException;
import com.hsf.hotel.exception.BusinessRuleException;
import com.hsf.hotel.exception.ResourceNotFoundException;
import com.hsf.hotel.admin.dto.AdminResponseDtos;
import com.hsf.hotel.room.model.Amenity;
import com.hsf.hotel.admin.model.AuditLog;
import com.hsf.hotel.booking.model.Booking;
import com.hsf.hotel.booking.model.BookingStatus;
import com.hsf.hotel.contact.model.ContactMessage;
import com.hsf.hotel.promotion.model.Promotion;
import com.hsf.hotel.review.model.Review;
import com.hsf.hotel.room.model.Room;
import com.hsf.hotel.room.model.RoomTypeEntity;
import com.hsf.hotel.user.model.User;
import com.hsf.hotel.voucher.model.Voucher;
import com.hsf.hotel.room.repository.RoomTypeRepository;
import com.hsf.hotel.voucher.repository.VoucherRepository;
import com.hsf.hotel.security.AuditActions;
import com.hsf.hotel.room.service.AmenityService;
import com.hsf.hotel.admin.service.AuditLogService;
import com.hsf.hotel.booking.service.BookingService;
import com.hsf.hotel.contact.service.ContactService;
import com.hsf.hotel.promotion.service.PromotionService;
import com.hsf.hotel.review.service.ReviewService;
import com.hsf.hotel.room.service.RoomService;
import com.hsf.hotel.room.service.RoomTypeService;
import com.hsf.hotel.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@com.hsf.hotel.config.ApiController
@RequestMapping(com.hsf.hotel.config.ApiPaths.V1 + "/admin")
@PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
public class AdminApi {

    private final BookingService bookingService;
    private final RoomService roomService;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomTypeService roomTypeService;
    private final AmenityService amenityService;
    private final VoucherRepository voucherRepository;
    private final ReviewService reviewService;
    private final UserService userService;
    private final PromotionService promotionService;
    private final ContactService contactService;
    private final AuditLogService auditLogService;

    public AdminApi(BookingService bookingService,
                    RoomService roomService,
                    RoomTypeRepository roomTypeRepository,
                    RoomTypeService roomTypeService,
                    AmenityService amenityService,
                    VoucherRepository voucherRepository,
                    ReviewService reviewService,
                    UserService userService,
                    PromotionService promotionService,
                    ContactService contactService,
                    AuditLogService auditLogService) {
        this.bookingService = bookingService;
        this.roomService = roomService;
        this.roomTypeRepository = roomTypeRepository;
        this.roomTypeService = roomTypeService;
        this.amenityService = amenityService;
        this.voucherRepository = voucherRepository;
        this.reviewService = reviewService;
        this.userService = userService;
        this.promotionService = promotionService;
        this.contactService = contactService;
        this.auditLogService = auditLogService;
    }

    private User requireAdmin(HttpSession session) {
        Object u = session.getAttribute("user");
        if (!(u instanceof User user) || !"ADMIN".equals(user.getRole())) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCodes.FORBIDDEN, "Admin only");
        }
        return user;
    }

    private User requireStaffOrAdmin(HttpSession session) {
        Object u = session.getAttribute("user");
        if (!(u instanceof User user)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, ErrorCodes.UNAUTHORIZED, "Please login");
        }
        if (!"ADMIN".equals(user.getRole())
                && !"MANAGER".equals(user.getRole())
                && !"STAFF".equals(user.getRole())) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCodes.FORBIDDEN, "Staff, Manager or Admin only");
        }
        return user;
    }

    private User requireManagerOrAdmin(HttpSession session) {
        Object u = session.getAttribute("user");
        if (!(u instanceof User user)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, ErrorCodes.UNAUTHORIZED, "Please login");
        }
        if (!"ADMIN".equals(user.getRole()) && !"MANAGER".equals(user.getRole())) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCodes.FORBIDDEN, "Manager or Admin only");
        }
        return user;
    }

    /* ---------------- dashboard ---------------- */

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<?>> dashboard(HttpSession session) {
        requireStaffOrAdmin(session);
        List<Booking> todayBookings = bookingService.getTodayBookings();
        BigDecimal monthlyRevenue = bookingService.getMonthlyRevenue();
        long totalUsers = userService.countAllUsers();

        Map<String, Object> data = new HashMap<>();
        data.put("totalRooms", roomService.countAll());
        data.put("availableRooms", roomService.countAvailable());
        data.put("totalBookings", bookingService.countAll());
        data.put("todayBookings", todayBookings.size());
        data.put("monthlyRevenue", monthlyRevenue);
        data.put("pendingBookings", bookingService.countByStatus(BookingStatus.PENDING_PAYMENT));
        data.put("totalUsers", totalUsers);
        data.put("totalReviews", reviewService.countAll());
        data.put("recentBookings", bookingService.getRecentBookings(5));
        data.put("mostBookedRooms", bookingService.getMostBookedRooms());
        data.put("mostRatingRooms", bookingService.getMostRatingRooms());
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    /* ---------------- bookings ---------------- */

    @GetMapping("/bookings")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<?>> bookings(@RequestParam(required = false) String status,
                                               HttpSession session) {
        requireStaffOrAdmin(session);
        List<Booking> bookings;
        try {
            bookings = (status != null && !status.isBlank())
                    ? bookingService.getRecentBookingsByStatus(BookingStatus.valueOf(status), 200)
                    : bookingService.getRecentBookings(200);
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.BAD_REQUEST,
                    "Trạng thái không hợp lệ: " + status);
        }
        Map<String, Object> data = new HashMap<>();

        data.put("bookings", bookings);
        data.put("statuses", BookingStatus.values());
        data.put("selectedStatus", status);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @PostMapping("/bookings/{id}/complete")
    public ResponseEntity<ApiResponse<?>> complete(@PathVariable Integer id, HttpSession session) {
        User admin = requireStaffOrAdmin(session);
        Booking booking = bookingService.markAsCompleted(id, admin);
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "message", "Đã đánh dấu hoàn thành đơn #" + id,
                "booking", booking
        )));
    }

    @PostMapping("/bookings/{id}/cancel")
    public ResponseEntity<ApiResponse<?>> adminCancel(@PathVariable Integer id, HttpSession session) {
        User admin = requireAdmin(session);
        Booking booking = bookingService.cancelBooking(id, admin);
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "message", "Đã hủy đặt phòng #" + id,
                "booking", booking,
                "refundAmount", booking.getRefundAmount(),
                "refundPercentage", booking.getRefundPercentage()
        )));
    }

    /* ---------------- rooms ---------------- */

    @GetMapping("/rooms")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<?>> rooms(HttpSession session) {
        requireManagerOrAdmin(session);
        Map<String, Object> data = new HashMap<>();
        data.put("rooms", roomService.getAllRooms().stream().map(AdminResponseDtos.AdminRoomResponse::from).toList());
        data.put("roomTypes", roomTypeRepository.findAll().stream().map(AdminResponseDtos.RoomTypeDto::from).toList());
        data.put("amenities", amenityService.getAllAmenities().stream().map(AdminResponseDtos.AmenityDto::from).toList());
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    public static class RoomPayload {
        public Integer id;
        public String roomNumber;
        public String roomType;        // accepted as either an ID or a name
        public Integer roomTypeId;     // explicit numeric id takes priority
        public BigDecimal pricePerNight;
        public String description;
        public String imageUrl;
        public Boolean isAvailable;
        public List<Integer> amenityIds;
        public Integer capacity;
        public Integer bedrooms;
        public List<String> galleryImages;
        public String houseRules;
        public String policies;
        public String nearbyAttractions;
        public String checkInTime;
        public String checkOutTime;
        public Double latitude;
        public Double longitude;
        public String nearbyRestaurants;
        public String nearbyCafes;
        public String nearbyAirport;
        public String directions;
        public Integer minimumStay;
        public Integer maximumStay;
    }

    @PostMapping("/rooms")
    @Transactional
    public ResponseEntity<ApiResponse<?>> saveRoom(@RequestBody RoomPayload p, HttpSession session) {
        requireManagerOrAdmin(session);
        if (p.roomNumber == null || p.roomNumber.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.BAD_REQUEST, "Thiếu số phòng");
        }
        if (p.pricePerNight == null || p.pricePerNight.signum() < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.BAD_REQUEST, "Giá phòng không hợp lệ");
        }
        if ((p.roomTypeId == null) && (p.roomType == null || p.roomType.isBlank())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.BAD_REQUEST, "Thiếu loại phòng");
        }

        Room room = (p.id != null)
                ? roomService.getRoomById(p.id).orElseThrow(() -> new ResourceNotFoundException("Room", p.id))
                : new Room();
        room.setRoomNumber(p.roomNumber.trim());
        // Prefer numeric id; fall back to name for backward compatibility with
        // older SPA payloads that still send `roomType` as a display name.
        RoomTypeEntity rt;
        if (p.roomTypeId != null) {
            rt = roomTypeService.getRoomTypeById(p.roomTypeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Room type", p.roomTypeId));
        } else {
            rt = roomTypeRepository.findByName(p.roomType)
                    .orElseThrow(() -> new ResourceNotFoundException("Room type", p.roomType));
        }
        room.setRoomType(rt);
        room.setPricePerNight(p.pricePerNight);
        room.setDescription(p.description);
        room.setImageUrl(p.imageUrl);
        room.setIsAvailable(p.isAvailable != null ? p.isAvailable : false);
        if (p.capacity != null) {
            room.setCapacity(p.capacity);
        }
        if (p.bedrooms != null) {
            room.setBedrooms(p.bedrooms);
        }
        if (p.amenityIds != null) {
            room.setAmenities(p.amenityIds.stream()
                    .map(aId -> amenityService.getAmenityById(aId).orElse(null))
                    .filter(a -> a != null).toList());
        } else if (p.id == null) {
            room.setAmenities(List.of());
        }
        
        if (p.galleryImages != null) {
            room.setGalleryImages(new java.util.ArrayList<>(p.galleryImages));
        }
        if (p.houseRules != null) room.setHouseRules(p.houseRules);
        if (p.policies != null) room.setPolicies(p.policies);
        if (p.nearbyAttractions != null) room.setNearbyAttractions(p.nearbyAttractions);
        if (p.checkInTime != null) room.setCheckInTime(p.checkInTime);
        if (p.checkOutTime != null) room.setCheckOutTime(p.checkOutTime);
        room.setLatitude(p.latitude);
        room.setLongitude(p.longitude);
        room.setNearbyRestaurants(p.nearbyRestaurants);
        room.setNearbyCafes(p.nearbyCafes);
        room.setNearbyAirport(p.nearbyAirport);
        room.setDirections(p.directions);
        if (p.minimumStay != null) room.setMinimumStay(p.minimumStay);
        if (p.maximumStay != null) room.setMaximumStay(p.maximumStay);
        Room saved = roomService.saveRoom(room);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("room", AdminResponseDtos.AdminRoomResponse.from(saved),
                "message", p.id != null ? "Cập nhật phòng thành công!" : "Thêm phòng mới thành công!")));
    }

    @DeleteMapping("/rooms/{id}")
    public ResponseEntity<ApiResponse<?>> deleteRoom(@PathVariable Integer id, HttpSession session) {
        requireManagerOrAdmin(session);
        try {
            roomService.deleteRoom(id);
            return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Xóa phòng thành công!")));
        } catch (BusinessRuleException e) {
            throw new ApiException(HttpStatus.CONFLICT, "ROOM_HAS_HISTORY", e.getMessage());
        }
    }

    /* ---------------- vouchers ---------------- */

    @GetMapping("/vouchers")
    public ResponseEntity<ApiResponse<?>> listVouchers(HttpSession session) {
        requireManagerOrAdmin(session);
        List<AdminResponseDtos.AdminVoucherResponse> vouchers = voucherRepository.findAll()
                .stream()
                .map(AdminResponseDtos.AdminVoucherResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(Map.of("vouchers", vouchers)));
    }

    public static class VoucherPayload {
        public Integer id;
        public String code;
        public BigDecimal amount;
        public String expiryDate;
        public Integer quantity;
        public Boolean percent;
    }

    @PostMapping("/vouchers")
    public ResponseEntity<ApiResponse<?>> saveVoucher(@RequestBody VoucherPayload p, HttpSession session) {
        requireManagerOrAdmin(session);
        if (p == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.BAD_REQUEST, "Thiếu payload");
        }
        if (p.code == null || p.code.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.BAD_REQUEST, "Thiếu mã voucher");
        }
        if (p.amount == null || p.amount.signum() < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.BAD_REQUEST, "Giá trị voucher không hợp lệ");
        }
        if (Boolean.TRUE.equals(p.percent) && p.amount.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.BAD_REQUEST,
                    "Phần trăm giảm giá không được vượt quá 100");
        }

        Voucher v = (p.id != null) ? voucherRepository.findById(p.id).orElse(new Voucher()) : new Voucher();
        v.setCode(p.code.trim().toUpperCase());
        v.setAmount(p.amount);
        if (p.expiryDate != null && !p.expiryDate.isBlank()) {
            try {
                v.setExpiryDate(LocalDate.parse(p.expiryDate));
            } catch (Exception ex) {
                throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.BAD_REQUEST,
                        "Định dạng ngày hết hạn không hợp lệ (yyyy-MM-dd)");
            }
        } else {
            v.setExpiryDate(null);
        }
        v.setQuantity(p.quantity != null ? p.quantity : 1);
        v.setPercent(p.percent != null ? p.percent : false);
        voucherRepository.save(v);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("voucher", v,
                "message", p.id != null ? "Cập nhật voucher thành công" : "Tạo voucher thành công")));
    }

    @DeleteMapping("/vouchers/{id}")
    public ResponseEntity<ApiResponse<?>> deleteVoucher(@PathVariable Integer id, HttpSession session) {
        requireManagerOrAdmin(session);
        try {
            voucherRepository.deleteById(id);
            return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Xóa voucher thành công")));
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "DELETE_FAILED", e.getMessage());
        }
    }

    /* ---------------- room types ---------------- */

    @GetMapping("/room-types")
    public ResponseEntity<ApiResponse<?>> roomTypes(HttpSession session) {
        requireManagerOrAdmin(session);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("roomTypes", roomTypeService.getAllRoomTypes())));
    }

    public static class RoomTypePayload {
        public Integer id;
        public String name;
        public String description;
    }

    @PostMapping("/room-types")
    public ResponseEntity<ApiResponse<?>> saveRoomType(@RequestBody RoomTypePayload p, HttpSession session) {
        requireManagerOrAdmin(session);
        if (p == null || p.name == null || p.name.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.BAD_REQUEST, "Thiếu tên loại phòng");
        }
        RoomTypeEntity rt = (p.id != null)
                ? roomTypeService.getRoomTypeById(p.id).orElseThrow(() -> new ResourceNotFoundException("Room type", p.id))
                : new RoomTypeEntity();
        rt.setName(p.name.trim().toUpperCase());
        rt.setDescription(p.description);
        roomTypeService.saveRoomType(rt);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("roomType", rt, "message", "Lưu Loại phòng thành công!")));
    }

    @DeleteMapping("/room-types/{id}")
    public ResponseEntity<ApiResponse<?>> deleteRoomType(@PathVariable Integer id, HttpSession session) {
        requireManagerOrAdmin(session);
        try {
            roomTypeService.deleteRoomType(id);
            return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Xóa Loại phòng thành công!")));
        } catch (Exception e) {
            throw new ApiException(HttpStatus.CONFLICT, "DELETE_FAILED",
                    "Không thể xóa Loại phòng đang được sử dụng!");
        }
    }

    /* ---------------- amenities ---------------- */

    @GetMapping("/amenities")
    public ResponseEntity<ApiResponse<?>> amenities(HttpSession session) {
        requireManagerOrAdmin(session);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("amenities", amenityService.getAllAmenities())));
    }

    public static class AmenityPayload {
        public Integer id;
        public String name;
        public String iconCode;
    }

    @PostMapping("/amenities")
    public ResponseEntity<ApiResponse<?>> saveAmenity(@RequestBody AmenityPayload p, HttpSession session) {
        requireManagerOrAdmin(session);
        if (p == null || p.name == null || p.name.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.BAD_REQUEST, "Thiếu tên tiện ích");
        }
        Amenity a = (p.id != null)
                ? amenityService.getAmenityById(p.id).orElseThrow(() -> new ResourceNotFoundException("Amenity", p.id))
                : new Amenity();
        a.setName(p.name.trim());
        a.setIconCode(p.iconCode);
        amenityService.saveAmenity(a);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("amenity", a, "message", "Lưu Tiện ích thành công!")));
    }

    @DeleteMapping("/amenities/{id}")
    public ResponseEntity<ApiResponse<?>> deleteAmenity(@PathVariable Integer id, HttpSession session) {
        requireManagerOrAdmin(session);
        try {
            amenityService.deleteAmenity(id);
            return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Xóa Tiện ích thành công!")));
        } catch (Exception e) {
            throw new ApiException(HttpStatus.CONFLICT, "DELETE_FAILED",
                    "Không thể xóa Tiện ích đang được sử dụng!");
        }
    }

    /* ---------------- reports ---------------- */

    @GetMapping("/reports")
    public ResponseEntity<ApiResponse<?>> reports(HttpSession session) {
        requireManagerOrAdmin(session);
        Map<String, Object> data = new HashMap<>();
        data.put("mostBookedRooms", bookingService.getMostBookedRooms());
        data.put("topRatedRooms", bookingService.getMostRatingRooms());
        data.put("totalReviews", reviewService.getAllReviews().size());
        data.put("totalUsers", userService.countAllUsers());
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    /* ---------------- check-in / check-out ---------------- */

    @PostMapping("/bookings/{id}/checkin")
    public ResponseEntity<ApiResponse<?>> checkIn(@PathVariable Integer id,
                                               HttpSession session,
                                               HttpServletRequest request) {
        User admin = requireStaffOrAdmin(session);
        Booking booking = bookingService.checkIn(id, admin);
        auditLogService.log(admin, AuditActions.BOOKING_CHECKIN, "Booking", id, "Admin checked in guest", request);
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "message", "Đã nhận phòng cho đơn #" + id,
                "booking", booking
        )));
    }

    @PostMapping("/bookings/{id}/checkout")
    public ResponseEntity<ApiResponse<?>> checkOut(@PathVariable Integer id,
                                                HttpSession session,
                                                HttpServletRequest request) {
        User admin = requireStaffOrAdmin(session);
        Booking booking = bookingService.checkOut(id, admin);
        auditLogService.log(admin, AuditActions.BOOKING_CHECKOUT, "Booking", id, "Admin checked out guest", request);
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "message", "Đã trả phòng cho đơn #" + id,
                "booking", booking
        )));
    }

    @PostMapping("/bookings/{id}/no-show")
    public ResponseEntity<ApiResponse<?>> markNoShow(@PathVariable Integer id,
                                                   HttpSession session,
                                                   HttpServletRequest request) {
        User staff = requireStaffOrAdmin(session);
        Booking booking = bookingService.markAsNoShow(id, staff);
        auditLogService.log(staff, AuditActions.BOOKING_NO_SHOW, "Booking", id,
                "Guest marked as no-show", request);
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "message", "Đã đánh dấu khách không đến cho đơn #" + id,
                "booking", booking
        )));
    }

    /* ---------------- today's operations ---------------- */

    @GetMapping("/bookings/today-checkin")
    public ResponseEntity<ApiResponse<?>> todayCheckIns(HttpSession session) {
        requireStaffOrAdmin(session);
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "bookings", bookingService.getTodayCheckIns()
        )));
    }

    @GetMapping("/bookings/today-checkout")
    public ResponseEntity<ApiResponse<?>> todayCheckOuts(HttpSession session) {
        requireStaffOrAdmin(session);
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "bookings", bookingService.getTodayCheckOuts()
        )));
    }

    /* ---------------- promotions (admin CRUD) ---------------- */

    @GetMapping("/promotions")
    public ResponseEntity<ApiResponse<?>> listPromotions(HttpSession session) {
        requireManagerOrAdmin(session);
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "promotions", promotionService.getAllPromotions()
        )));
    }

    @PostMapping("/promotions")
    public ResponseEntity<ApiResponse<?>> savePromotion(@RequestBody Promotion promo,
                                                      HttpSession session,
                                                      HttpServletRequest request) {
        User admin = requireManagerOrAdmin(session);
        Promotion saved;
        if (promo.getId() != null) {
            saved = promotionService.updatePromotion(promo.getId(), promo);
        } else {
            saved = promotionService.createPromotion(promo);
        }
        auditLogService.log(admin, AuditActions.ADMIN_PROMOTION_CHANGE, "Promotion", saved.getId(), saved.getTitle(), request);
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "promotion", saved,
                "message", promo.getId() != null ? "Cập nhật khuyến mãi thành công" : "Tạo khuyến mãi thành công"
        )));
    }

    @DeleteMapping("/promotions/{id}")
    public ResponseEntity<ApiResponse<?>> deletePromotion(@PathVariable Integer id, HttpSession session, HttpServletRequest request) {
        User admin = requireManagerOrAdmin(session);
        promotionService.deletePromotion(id);
        auditLogService.log(admin, AuditActions.ADMIN_PROMOTION_CHANGE, "Promotion", id, "Deleted promotion", request);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Xóa khuyến mãi thành công")));
    }

    /* ---------------- review moderation ---------------- */

    @GetMapping("/reviews")
    public ResponseEntity<ApiResponse<?>> listReviews(HttpSession session) {
        requireManagerOrAdmin(session);
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "reviews", reviewService.getAllReviews()
        )));
    }

    @PostMapping("/reviews/{id}/reply")
    public ResponseEntity<ApiResponse<?>> replyToReview(@PathVariable Integer id,
                                                      @RequestBody Map<String, String> body,
                                                      HttpSession session,
                                                      HttpServletRequest request) {
        User admin = requireManagerOrAdmin(session);
        String reply = body != null ? body.get("reply") : null;
        if (reply == null || reply.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.BAD_REQUEST, "Thiếu nội dung phản hồi");
        }
        reviewService.replyToReview(id, reply);
        auditLogService.log(admin, AuditActions.ADMIN_REVIEW_MODERATE, "Review", id, "Admin replied to review", request);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Đã phản hồi đánh giá")));
    }

    @PostMapping("/reviews/{id}/hide")
    public ResponseEntity<ApiResponse<?>> hideReview(@PathVariable Integer id, HttpSession session, HttpServletRequest request) {
        User admin = requireManagerOrAdmin(session);
        reviewService.hideReview(id);
        auditLogService.log(admin, AuditActions.ADMIN_REVIEW_MODERATE, "Review", id, "Admin hid review", request);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Đã ẩn đánh giá")));
    }

    @DeleteMapping("/reviews/{id}")
    public ResponseEntity<ApiResponse<?>> deleteReview(@PathVariable Integer id, HttpSession session, HttpServletRequest request) {
        User admin = requireManagerOrAdmin(session);
        reviewService.deleteReview(admin, id);
        auditLogService.log(admin, AuditActions.ADMIN_REVIEW_MODERATE, "Review", id, "Admin deleted review", request);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Đã xóa đánh giá")));
    }

    /* ---------------- contact messages ---------------- */

    @GetMapping("/contacts")
    public ResponseEntity<ApiResponse<?>> listContacts(HttpSession session) {
        requireStaffOrAdmin(session);
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "contacts", contactService.getAllContacts()
        )));
    }

    public static class ContactStatusPayload {
        public String status;
        public String notes;
        public Integer assignedTo;
    }

    @PutMapping("/contacts/{id}/status")
    public ResponseEntity<ApiResponse<?>> updateContactStatus(@PathVariable Integer id,
                                                            @RequestBody(required = false) ContactStatusPayload body,
                                                            HttpSession session,
                                                            HttpServletRequest request) {
        User admin = requireStaffOrAdmin(session);
        if (body == null || body.status == null || body.status.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.BAD_REQUEST, "Thiếu trường 'status'");
        }
        try {
            contactService.updateStatus(id,
                    com.hsf.hotel.contact.model.ContactMessage.ContactStatus.valueOf(body.status),
                    body.notes, body.assignedTo);
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.BAD_REQUEST,
                    "Trạng thái không hợp lệ: " + body.status);
        }
        auditLogService.log(admin, AuditActions.ADMIN_USER_ROLE_CHANGE, "ContactMessage", id,
                "Admin updated contact status to " + body.status, request);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Cập nhật liên hệ thành công")));
    }

    /* ---------------- analytics: revenue / occupancy ---------------- */

    @GetMapping("/revenue")
    public ResponseEntity<ApiResponse<?>> revenueTimeseries(
            @RequestParam("startDate") String startDateStr,
            @RequestParam("endDate") String endDateStr,
            HttpSession session) {
        requireManagerOrAdmin(session);
        LocalDate startDate;
        LocalDate endDate;
        try {
            startDate = LocalDate.parse(startDateStr);
            endDate = LocalDate.parse(endDateStr);
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.BAD_REQUEST,
                    "Định dạng ngày không hợp lệ (yyyy-MM-dd)");
        }
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "series", bookingService.getRevenueTimeseries(startDate, endDate)
        )));
    }

    @GetMapping("/occupancy")
    public ResponseEntity<ApiResponse<?>> occupancyTimeseries(
            @RequestParam("startDate") String startDateStr,
            @RequestParam("endDate") String endDateStr,
            HttpSession session) {
        requireManagerOrAdmin(session);
        LocalDate startDate;
        LocalDate endDate;
        try {
            startDate = LocalDate.parse(startDateStr);
            endDate = LocalDate.parse(endDateStr);
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.BAD_REQUEST,
                    "Định dạng ngày không hợp lệ (yyyy-MM-dd)");
        }
        int totalRooms = roomService.getAllRooms().size();
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "series", bookingService.getOccupancyTimeseries(startDate, endDate, totalRooms),
                "totalRooms", totalRooms
        )));
    }

    /* ---------------- audit logs ---------------- */

    @GetMapping("/audit-logs")
    public ResponseEntity<ApiResponse<?>> auditLogs(
            @RequestParam(required = false) Integer userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            HttpSession session) {
        requireAdmin(session);
        if (size <= 0 || size > 200) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.BAD_REQUEST,
                    "size phải trong khoảng 1..200");
        }
        org.springframework.data.domain.Pageable pageable =
                org.springframework.data.domain.PageRequest.of(page, size);
        org.springframework.data.domain.Page<com.hsf.hotel.admin.model.AuditLog> result =
                (userId != null)
                        ? auditLogService.getLogsByUser(userId, pageable)
                        : auditLogService.getRecentLogs(pageable);
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "logs", result.getContent(),
                "page", result.getNumber(),
                "size", result.getSize(),
                "totalElements", result.getTotalElements(),
                "totalPages", result.getTotalPages()
        )));
    }
    /* ---------------- refunds ---------------- */

    @GetMapping("/refunds")
    public ResponseEntity<ApiResponse<?>> listRefunds(HttpSession session) {
        requireAdmin(session);
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "bookings", bookingService.getRefundedBookings()
        )));
    }

    /* ---------------- villa maintenances ---------------- */

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.hsf.hotel.room.service.VillaMaintenanceService villaMaintenanceService;

    public static class ScheduleMaintenanceRequest {
        public Integer roomId;
        public LocalDate startDate;
        public LocalDate endDate;
        public String reason;
    }

    @GetMapping("/maintenances")
    public ResponseEntity<ApiResponse<?>> listMaintenances(
            @RequestParam(required = false) Integer roomId,
            HttpSession session) {
        requireManagerOrAdmin(session);
        if (villaMaintenanceService == null) {
            return ResponseEntity.ok(ApiResponse.ok(List.of()));
        }
        var list = roomId != null
                ? villaMaintenanceService.getMaintenancesForRoom(roomId)
                : villaMaintenanceService.getAllMaintenances();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @PostMapping("/maintenances")
    public ResponseEntity<ApiResponse<?>> scheduleMaintenance(
            @RequestBody ScheduleMaintenanceRequest req,
            HttpSession session) {
        User admin = requireManagerOrAdmin(session);
        if (villaMaintenanceService == null) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", "Maintenance service unavailable");
        }
        var maint = villaMaintenanceService.scheduleMaintenance(
                req.roomId, req.startDate, req.endDate, req.reason, admin);
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "message", "Đã lên lịch bảo trì biệt thự thành công",
                "maintenance", maint
        )));
    }

    @DeleteMapping("/maintenances/{id}")
    public ResponseEntity<ApiResponse<?>> cancelMaintenance(
            @PathVariable Integer id,
            HttpSession session) {
        requireManagerOrAdmin(session);
        if (villaMaintenanceService != null) {
            villaMaintenanceService.cancelMaintenance(id);
        }
        return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Đã hủy lịch bảo trì")));
    }
}
