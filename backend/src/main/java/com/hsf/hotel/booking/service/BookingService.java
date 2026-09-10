package com.hsf.hotel.booking.service;
import com.hsf.hotel.voucher.service.VoucherService;
import com.hsf.hotel.payment.model.Payment;
import com.hsf.hotel.notification.service.NotificationService;
import com.hsf.hotel.notification.service.NotificationProducer;

import com.hsf.hotel.booking.dto.BookingDTO;
import com.hsf.hotel.notification.dto.NotificationEvent;
import com.hsf.hotel.exception.BusinessRuleException;
import com.hsf.hotel.exception.ForbiddenException;
import com.hsf.hotel.exception.ResourceNotFoundException;
import com.hsf.hotel.booking.model.Booking;
import com.hsf.hotel.booking.model.BookingStatus;
import com.hsf.hotel.notification.model.NotificationType;
import com.hsf.hotel.room.model.Room;
import com.hsf.hotel.user.model.User;
import com.hsf.hotel.voucher.model.Voucher;
import com.hsf.hotel.booking.model.BookingStatusTransition;
import com.hsf.hotel.booking.repository.BookingRepository;
import com.hsf.hotel.booking.repository.BookingStatusTransitionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import com.hsf.hotel.room.dto.RoomStatsDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Domain logic for villa bookings. Encapsulates the booking workflow
 * (PENDING_PAYMENT -> PAID -> CHECKED_IN -> CHECKED_OUT -> COMPLETED)
 * and the cancellation refund policy.
 *
 * <p>Race conditions are mitigated by:
 * <ul>
 *   <li>A pessimistic row-lock on the room when checking availability.</li>
 *   <li>All voucher mutations inside the same transaction as the booking.</li>
 * </ul>
 */
@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);
    private static final BigDecimal DAY_USE_PRICE_FACTOR = new BigDecimal("0.5");
    private static final int FULL_REFUND_DAYS = 3;
    private static final int PARTIAL_REFUND_DAYS = 1;
    private static final int FULL_REFUND_PERCENT = 100;
    private static final int PARTIAL_REFUND_PERCENT = 50;
    private static final BigDecimal PERCENT_DIVISOR = BigDecimal.valueOf(100);
    private static final BigDecimal SERVICE_FEE_RATE = new BigDecimal("0.08"); // 8%
    private static final BigDecimal TAX_RATE = new BigDecimal("0.10"); // 10% VAT

    private final BookingRepository bookingRepository;
    private final BookingStatusTransitionRepository transitionRepository;
    private final NotificationProducer notificationProducer;
    private final VoucherService voucherService;
    private final EntityManager entityManager;
    private final NotificationService notificationService;
    private final com.hsf.hotel.room.repository.VillaMaintenanceRepository maintenanceRepository;
    private final int paymentDeadlineHours;
    private final String adminEmail;

    public BookingService(BookingRepository bookingRepository,
                          BookingStatusTransitionRepository transitionRepository,
                          NotificationProducer notificationProducer,
                          VoucherService voucherService,
                          EntityManager entityManager,
                          NotificationService notificationService,
                          @org.springframework.beans.factory.annotation.Autowired(required = false)
                          com.hsf.hotel.room.repository.VillaMaintenanceRepository maintenanceRepository,
                          @Value("${app.booking.payment-deadline-hours:24}") int paymentDeadlineHours,
                          @Value("${app.admin.email:admin@hotel.com}") String adminEmail) {
        this.bookingRepository = bookingRepository;
        this.transitionRepository = transitionRepository;
        this.notificationProducer = notificationProducer;
        this.voucherService = voucherService;
        this.entityManager = entityManager;
        this.notificationService = notificationService;
        this.maintenanceRepository = maintenanceRepository;
        this.paymentDeadlineHours = paymentDeadlineHours;
        this.adminEmail = adminEmail;
    }

    /* ---------------- queries ---------------- */

    public List<Booking> getUserBookings(User user) {
        return bookingRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public Optional<Booking> getBookingById(Integer id) {
        return bookingRepository.findById(id);
    }

    public List<Booking> getAllBookings() {
        return bookingRepository.findAllByOrderByCreatedAtDesc();
    }

    public long countAll() {
        return bookingRepository.count();
    }

    public long countByStatus(BookingStatus status) {
        return bookingRepository.countByStatus(status);
    }

    public List<Booking> getRecentBookings(int limit) {
        return bookingRepository.findAll(org.springframework.data.domain.PageRequest.of(0, limit, org.springframework.data.domain.Sort.by("createdAt").descending())).getContent();
    }
    
    public List<Booking> getRecentBookingsByStatus(BookingStatus status, int limit) {
        return bookingRepository.findByStatus(status, org.springframework.data.domain.PageRequest.of(0, limit, org.springframework.data.domain.Sort.by("createdAt").descending())).getContent();
    }

    public List<Booking> getTodayBookings() {
        return bookingRepository.findByCheckInDate(LocalDate.now());
    }

    public List<Booking> getBookingsByStatus(BookingStatus status) {
        return bookingRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    public List<RoomStatsDto> getMostBookedRooms() {
        return bookingRepository.findMostBookedRooms();
    }

    public List<RoomStatsDto> getMostRatingRooms() {
        return bookingRepository.findMostratingRooms();
    }

    public BigDecimal getMonthlyRevenue() {
        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate endOfMonth = startOfMonth.plusMonths(1);
        return bookingRepository.findSuccessfulBookingsInDateRange(startOfMonth, endOfMonth).stream()
                .map(Booking::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /* ---------------- pricing & availability ---------------- */

    /** Pricing breakdown record returned by calculatePricing. */
    public record PricingBreakdown(
            BigDecimal subtotal,
            BigDecimal serviceFee,
            BigDecimal taxAmount,
            BigDecimal total
    ) {}

    /** Legacy method — returns total only. */
    public BigDecimal calculateTotalPrice(Room room, LocalDate checkIn, LocalDate checkOut) {
        return calculatePricing(room, checkIn, checkOut).total();
    }

    /** Full pricing breakdown: subtotal + service fee + tax = total. */
    public PricingBreakdown calculatePricing(Room room, LocalDate checkIn, LocalDate checkOut) {
        return calculatePricing(room, checkIn, checkOut, null);
    }

    public PricingBreakdown calculatePricing(Room room, LocalDate checkIn, LocalDate checkOut, Integer guests) {
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        BigDecimal subtotal = BigDecimal.ZERO;
        if (nights == 0) {
            subtotal = room.getPricePerNight().multiply(DAY_USE_PRICE_FACTOR);
        } else {
            LocalDate current = checkIn;
            while (current.isBefore(checkOut)) {
                java.time.DayOfWeek dow = current.getDayOfWeek();
                BigDecimal nightRate = room.getPricePerNight();
                // Weekend surcharge (+15% on Friday and Saturday nights)
                if (dow == java.time.DayOfWeek.FRIDAY || dow == java.time.DayOfWeek.SATURDAY) {
                    nightRate = nightRate.multiply(new BigDecimal("1.15")).setScale(0, RoundingMode.HALF_UP);
                }
                subtotal = subtotal.add(nightRate);
                current = current.plusDays(1);
            }
            // Extra guest fee ($25/night/guest for guests beyond base 2)
            if (guests != null && guests > 2 && room.getCapacity() != null && room.getCapacity() > 2) {
                int extraGuests = guests - 2;
                BigDecimal extraGuestFee = BigDecimal.valueOf(extraGuests)
                        .multiply(new BigDecimal("25"))
                        .multiply(BigDecimal.valueOf(nights));
                subtotal = subtotal.add(extraGuestFee);
            }
        }
        BigDecimal serviceFee = subtotal.multiply(SERVICE_FEE_RATE).setScale(0, RoundingMode.HALF_UP);
        BigDecimal taxAmount = subtotal.multiply(TAX_RATE).setScale(0, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(serviceFee).add(taxAmount);
        return new PricingBreakdown(subtotal, serviceFee, taxAmount, total);
    }

    /**
     * Re-checks availability inside a transaction with a pessimistic write lock
     * on the room row to prevent the classic "two users book the same dates"
     * race condition. Also checks against scheduled villa maintenance.
     */
    public boolean isRoomAvailable(Room room, LocalDate checkIn, LocalDate checkOut) {
        if (!Boolean.TRUE.equals(room.getIsAvailable())) {
            return false;
        }
        boolean hasBookingConflict = !bookingRepository.findConflictingBookings(room, checkIn, checkOut).isEmpty();
        if (hasBookingConflict) return false;

        if (maintenanceRepository != null) {
            boolean hasMaintenanceConflict = !maintenanceRepository.findConflictingMaintenances(room, checkIn, checkOut).isEmpty();
            if (hasMaintenanceConflict) return false;
        }
        return true;
    }

    /* ---------------- mutations ---------------- */

    /**
     * Persist a new booking with optional voucher. Performed in a single
     * transaction so availability, voucher consumption, and audit fields all
     * commit together.
     */
    @Transactional
    public Booking createBooking(User user, Room room, LocalDate checkIn, LocalDate checkOut,
                                 String guestName, String guestPhone, String guestEmail,
                                 Integer guests, String notes, String voucherCode) {

        validateDates(checkIn, checkOut);

        Room lockedRoom = entityManager.find(Room.class, room.getId(), LockModeType.PESSIMISTIC_WRITE);
        if (lockedRoom == null) {
            throw new ResourceNotFoundException("Room", room.getId());
        }

        List<Booking> conflicts = bookingRepository.findConflictingBookingsForUpdate(lockedRoom, checkIn, checkOut);
        if (!conflicts.isEmpty() || !Boolean.TRUE.equals(lockedRoom.getIsAvailable())) {
            throw new BusinessRuleException("ROOM_UNAVAILABLE", "Phòng đã được đặt trong khoảng thời gian này");
        }

        if (maintenanceRepository != null) {
            var maintConflicts = maintenanceRepository.findConflictingMaintenances(lockedRoom, checkIn, checkOut);
            if (!maintConflicts.isEmpty()) {
                throw new BusinessRuleException("ROOM_IN_MAINTENANCE", "Phòng đang trong lịch bảo trì vào thời gian này");
            }
        }

        if (guests != null && lockedRoom.getCapacity() != null && guests > lockedRoom.getCapacity()) {
            throw new BusinessRuleException("GUEST_LIMIT_EXCEEDED",
                    "Số khách vượt quá sức chứa của phòng (tối đa " + lockedRoom.getCapacity() + ")");
        }

        PricingBreakdown pricing = calculatePricing(lockedRoom, checkIn, checkOut, guests);
        String appliedCode = null;
        BigDecimal discount = BigDecimal.ZERO;

        if (voucherCode != null && !voucherCode.trim().isEmpty()) {
            VoucherService.VoucherValidationResult vres = voucherService.validateVoucher(voucherCode);
            if (!vres.valid) {
                throw new BusinessRuleException("VOUCHER_INVALID",
                        vres.message != null ? vres.message : "Voucher không hợp lệ");
            }
            if (vres.voucher != null) {
                Voucher v = vres.voucher;
                if (Boolean.TRUE.equals(v.getPercent())) {
                    discount = pricing.subtotal().multiply(v.getAmount()).divide(BigDecimal.valueOf(100));
                } else {
                    discount = v.getAmount();
                }
                if (discount.compareTo(pricing.subtotal()) > 0) {
                    discount = pricing.subtotal();
                }
                appliedCode = v.getCode();
                boolean consumed = voucherService.consumeVoucher(v);
                if (!consumed) {
                    throw new BusinessRuleException("VOUCHER_EXHAUSTED",
                            "Voucher vừa được sử dụng bởi giao dịch khác, vui lòng thử lại");
                }
            } else if (vres.promotion != null) {
                com.hsf.hotel.promotion.model.Promotion p = vres.promotion;
                if (p.getRooms() != null && !p.getRooms().isEmpty() && !p.getRooms().contains(lockedRoom)) {
                    throw new BusinessRuleException("PROMOTION_INVALID_FOR_ROOM",
                            "Mã ưu đãi này không áp dụng cho villa đã chọn");
                }
                long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
                if (nights < p.getMinimumNights()) {
                    throw new BusinessRuleException("PROMOTION_MIN_NIGHTS_NOT_MET",
                            "Mã ưu đãi yêu cầu đặt tối thiểu " + p.getMinimumNights() + " đêm");
                }
                if (pricing.subtotal().compareTo(p.getMinimumBookingAmount()) < 0) {
                    throw new BusinessRuleException("PROMOTION_MIN_AMOUNT_NOT_MET",
                            "Mã ưu đãi yêu cầu giá trị đặt phòng tối thiểu từ " + p.getMinimumBookingAmount());
                }

                if (p.getDiscountPercent() != null) {
                    discount = pricing.subtotal().multiply(p.getDiscountPercent()).divide(BigDecimal.valueOf(100));
                } else if (p.getDiscountAmount() != null) {
                    discount = p.getDiscountAmount();
                }
                if (discount.compareTo(pricing.subtotal()) > 0) {
                    discount = pricing.subtotal();
                }
                appliedCode = p.getPromoCode();

                int updated = entityManager.createQuery("UPDATE Promotion p SET p.currentUses = COALESCE(p.currentUses, 0) + 1 WHERE p.id = :id AND (p.maximumUses IS NULL OR COALESCE(p.currentUses, 0) < p.maximumUses)")
                        .setParameter("id", p.getId())
                        .executeUpdate();
                if (updated == 0) {
                    throw new BusinessRuleException("PROMOTION_EXHAUSTED", "Mã ưu đãi đã hết lượt sử dụng");
                }
            }
        }

        BigDecimal finalPrice = pricing.subtotal().subtract(discount).add(pricing.serviceFee()).add(pricing.taxAmount());
        // Never let the discount drive the total below zero — this protects
        // against promotions whose nominal value exceeds the subtotal and
        // against coupon branches that forget to cap.
        if (finalPrice.signum() < 0) {
            finalPrice = BigDecimal.ZERO;
        }

        LocalDateTime now = LocalDateTime.now();
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setRoom(lockedRoom);
        booking.setCheckInDate(checkIn);
        booking.setCheckOutDate(checkOut);
        booking.setGuestName(guestName);
        booking.setGuestPhone(guestPhone);
        booking.setGuestEmail(guestEmail);
        booking.setGuests(guests);
        booking.setNotes(notes);
        booking.setStatus(BookingStatus.PENDING_PAYMENT);
        // HoldExpiresAt bounds the in-memory reservation; paymentDeadline is the
        // hard deadline after which the scheduler will flip the booking to
        // EXPIRED. They are intentionally separate: a hold may be released
        // earlier than payment failure (e.g. abandoned checkout).
        booking.setHoldExpiresAt(now.plusMinutes(15));
        booking.setPaymentDeadline(now.plusHours(paymentDeadlineHours));
        booking.setSubtotalPrice(pricing.subtotal());
        booking.setServiceFee(pricing.serviceFee());
        booking.setTaxAmount(pricing.taxAmount());
        booking.setTotalPrice(finalPrice);
        booking.setAppliedVoucherCode(appliedCode);
        booking.setDiscountAmount(discount);

        Booking saved = bookingRepository.save(booking);
        recordTransition(saved, null, BookingStatus.PENDING_PAYMENT, user, "Created new booking in PENDING_PAYMENT state");
        log.info("New booking #{} created for user {} on room {} ({}->{}) total={}",
                saved.getId(), user.getUsername(), lockedRoom.getRoomNumber(), checkIn, checkOut, finalPrice);
        NotificationEvent event = new NotificationEvent("NEW_BOOKING_ADMIN", saved.getId());
        event.setAdminEmail(adminEmail);
        notificationProducer.sendEmailNotification(event);
        return saved;
    }

    @Transactional
    public Booking saveBooking(Booking booking) {
        return bookingRepository.save(booking);
    }

    @Transactional
    public Booking approveBooking(Integer bookingId, User adminUser) {
        Booking booking = loadOrThrow(bookingId);
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new BusinessRuleException("INVALID_STATE", "Chỉ có thể duyệt đơn đang chờ xử lý hoặc tạm giữ");
        }
        BookingStatus oldStatus = booking.getStatus();
        booking.setStatus(BookingStatus.PAID);
        booking.setApprovedBy(adminUser);
        booking.setApprovedAt(LocalDateTime.now());
        booking.setPaidAt(LocalDateTime.now());

        Booking saved = bookingRepository.save(booking);
        recordTransition(saved, oldStatus, BookingStatus.PAID, adminUser, "Approved by admin");
        log.info("Booking #{} approved by {}", bookingId, adminUser.getUsername());
        NotificationEvent event = new NotificationEvent("BOOKING_APPROVED", saved.getId());
        notificationProducer.sendEmailNotification(event);
        return saved;
    }

    @Transactional
    public Booking rejectBooking(Integer bookingId, User adminUser, String reason) {
        Booking booking = loadOrThrow(bookingId);
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new BusinessRuleException("INVALID_STATE", "Chỉ có thể từ chối đơn đang chờ xử lý hoặc tạm giữ");
        }
        BookingStatus oldStatus = booking.getStatus();
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setApprovedBy(adminUser);
        booking.setApprovedAt(LocalDateTime.now());
        booking.setRejectionReason(reason != null ? reason : "Không đủ điều kiện");

        Booking saved = bookingRepository.save(booking);
        recordTransition(saved, oldStatus, BookingStatus.CANCELLED, adminUser, reason != null ? reason : "Không đủ điều kiện");
        log.info("Booking #{} rejected by {} - {}", bookingId, adminUser.getUsername(), saved.getRejectionReason());
        NotificationEvent event = new NotificationEvent("BOOKING_REJECTED", saved.getId());
        notificationProducer.sendEmailNotification(event);
        return saved;
    }

    @Transactional
    public Booking markAsCompleted(Integer bookingId, User adminUser) {
        Booking booking = loadOrThrow(bookingId);
        if (booking.getStatus() != BookingStatus.CHECKED_OUT) {
            throw new BusinessRuleException("INVALID_STATE",
                    "Chỉ có thể hoàn thành đơn đã trả phòng (hiện tại: " + booking.getStatus() + ")");
        }
        if (booking.getCheckOutDate().isAfter(LocalDate.now())) {
            throw new BusinessRuleException("TOO_EARLY",
                    "Chưa đến ngày trả phòng, không thể đánh dấu hoàn thành");
        }
        BookingStatus oldStatus = booking.getStatus();
        booking.setStatus(BookingStatus.COMPLETED);
        Booking saved = bookingRepository.save(booking);
        recordTransition(saved, oldStatus, BookingStatus.COMPLETED, adminUser, "Marked as completed by admin");
        log.info("Booking #{} marked as completed by {}", bookingId, adminUser.getUsername());
        return saved;
    }

    @Transactional
    public Booking confirmPayment(Integer bookingId, String transactionRef, BigDecimal amount) {
        Booking booking = loadOrThrow(bookingId);
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new BusinessRuleException("INVALID_STATE",
                    "Đơn không ở trạng thái hợp lệ để thanh toán (hiện tại: " + booking.getStatus() + ")");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessRuleException("INVALID_PAYMENT_AMOUNT", "Số tiền thanh toán phải lớn hơn 0");
        }
        BigDecimal expectedAmount = booking.getTotalPrice() != null ? booking.getTotalPrice() : BigDecimal.ZERO;
        if (expectedAmount.compareTo(amount) != 0) {
            throw new BusinessRuleException("PAYMENT_AMOUNT_MISMATCH",
                    "Số tiền thanh toán không khớp với tổng giá trị đơn");
        }
        BookingStatus oldStatus = booking.getStatus();
        booking.setStatus(BookingStatus.PAID);
        booking.setPaidAt(LocalDateTime.now());
        if (transactionRef != null) {
            booking.setNotes(
                    (booking.getNotes() != null ? booking.getNotes() + "\n" : "")
                            + "Payment ref: " + transactionRef);
        }
        Booking saved = bookingRepository.save(booking);
        recordTransition(saved, oldStatus, BookingStatus.PAID, null, "Payment confirmed: " + transactionRef);
        log.info("Payment confirmed for booking #{} (ref={}) amount={}",
                bookingId, transactionRef, amount);
        NotificationEvent event = new NotificationEvent("PAYMENT_ADMIN", saved.getId());
        event.setAdminEmail(adminEmail);
        notificationProducer.sendEmailNotification(event);
        return saved;
    }

    public List<Booking> getExpiredPaymentBookings() {
        return bookingRepository.findExpiredPaymentBookings(LocalDateTime.now());
    }

    /**
     * Expire a held booking whose payment deadline has elapsed. Called by the
     * scheduler. The booking is flipped to {@link BookingStatus#EXPIRED},
     * audited, the consumed voucher unit is returned, and the user is
     * notified by email + in-app banner. No-op if the booking is no longer
     * in PENDING_PAYMENT (avoids racing with a late payment confirmation).
     */
    @Transactional
    public Booking expireHold(Integer bookingId) {
        Booking booking = loadOrThrow(bookingId);
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            return booking;
        }
        BookingStatus oldStatus = booking.getStatus();

        if (booking.getAppliedVoucherCode() != null) {
            try {
                voucherService.releaseVoucherByCode(booking.getAppliedVoucherCode());
            } catch (Exception ex) {
                log.warn("Failed to release voucher {} while expiring booking #{}: {}",
                        booking.getAppliedVoucherCode(), bookingId, ex.getMessage());
            }
        }

        booking.setStatus(BookingStatus.EXPIRED);
        booking.setCancelledAt(LocalDateTime.now());
        booking.setCancelledBy("system-scheduler");
        booking.setCancellationReason(booking.getCancellationReason() != null
                ? booking.getCancellationReason()
                : "Auto-expired: payment deadline reached");
        Booking saved = bookingRepository.save(booking);
        recordTransition(saved, oldStatus, BookingStatus.EXPIRED, null,
                "Hold auto-expired by scheduler");
        return saved;
    }

    /**
     * Promote a checked-out booking whose checkout date is in the past to
     * {@link BookingStatus#COMPLETED}. No-op if the booking has already moved
     * on (e.g. an admin marked it manually).
     */
    @Transactional
    public Booking completeOverdueBooking(Integer bookingId) {
        Booking booking = loadOrThrow(bookingId);
        if (booking.getStatus() != BookingStatus.CHECKED_OUT) {
            return booking;
        }
        BookingStatus oldStatus = booking.getStatus();
        booking.setStatus(BookingStatus.COMPLETED);
        Booking saved = bookingRepository.save(booking);
        recordTransition(saved, oldStatus, BookingStatus.COMPLETED, null,
                "Auto-completed by scheduler");
        return saved;
    }

    /**
     * Promote a paid booking whose checkout date is in the past without an
     * intervening check-in to {@link BookingStatus#NO_SHOW}. This is the
     * inverse of {@link #markAsNoShow(Integer, User)} but acts on a set of
     * rows the scheduler discovers on its own.
     */
    @Transactional
    public Booking markNoShowOverdue(Integer bookingId) {
        Booking booking = loadOrThrow(bookingId);
        if (booking.getStatus() != BookingStatus.PAID) {
            return booking;
        }
        BookingStatus oldStatus = booking.getStatus();
        booking.setStatus(BookingStatus.NO_SHOW);
        Booking saved = bookingRepository.save(booking);
        recordTransition(saved, oldStatus, BookingStatus.NO_SHOW, null,
                "No-show flagged by scheduler");
        return saved;
    }

    @Transactional
    public Booking cancelBooking(Integer bookingId, User user) {
        return cancelBooking(bookingId, user, "Cancelled by user");
    }

    @Transactional
    public Booking cancelBooking(Integer bookingId, User user, String reason) {
        return cancelBooking(bookingId, user, reason, null, null, null);
    }

    @Transactional
    public Booking cancelBooking(Integer bookingId, User user, String reason,
                                 String refundBankName, String refundAccountNumber, String refundAccountName) {
        Booking booking = loadOrThrow(bookingId);
        if (!booking.getUser().getId().equals(user.getId()) && !"ADMIN".equals(user.getRole())) {
            throw new ForbiddenException("Bạn không có quyền hủy đơn này");
        }
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BusinessRuleException("ALREADY_CANCELLED", "Đơn đặt phòng đã được hủy trước đó");
        }
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new BusinessRuleException("ALREADY_COMPLETED", "Không thể hủy đơn đã hoàn thành");
        }
        BookingStatus oldStatus = booking.getStatus();

        // If the booking consumed a voucher unit but the charge never
        // settled, return the unit so the next user can use the code.
        if (booking.getAppliedVoucherCode() != null
                && oldStatus == BookingStatus.PENDING_PAYMENT) {
            try {
                voucherService.releaseVoucherByCode(booking.getAppliedVoucherCode());
            } catch (Exception ex) {
                log.warn("Failed to release voucher {} during cancel of booking #{}: {}",
                        booking.getAppliedVoucherCode(), bookingId, ex.getMessage());
            }
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now());
        booking.setCancelledBy(user != null ? user.getUsername() : "system");
        if (refundBankName != null) booking.setRefundBankName(refundBankName);
        if (refundAccountNumber != null) booking.setRefundAccountNumber(refundAccountNumber);
        if (refundAccountName != null) booking.setRefundAccountName(refundAccountName);
        calculateRefund(booking);
        Booking saved = bookingRepository.save(booking);
        recordTransition(saved, oldStatus, BookingStatus.CANCELLED, user, reason);
        return saved;
    }

    @Transactional
    public Booking updateBookingStatus(Integer bookingId, BookingStatus newStatus) {
        return updateBookingStatus(bookingId, newStatus, null);
    }

    @Transactional
    public Booking updateBookingStatus(Integer bookingId, BookingStatus newStatus, User actor) {
        Booking booking = loadOrThrow(bookingId);
        validateStatusTransition(booking.getStatus(), newStatus);
        BookingStatus oldStatus = booking.getStatus();
        booking.setStatus(newStatus);
        if (newStatus == BookingStatus.CANCELLED) {
            calculateRefund(booking);
            booking.setCancelledAt(LocalDateTime.now());
            booking.setCancelledBy(actor != null ? actor.getUsername() : "system");
        }
        if (newStatus == BookingStatus.CHECKED_IN && booking.getCheckedInAt() == null) {
            booking.setCheckedInAt(LocalDateTime.now());
        }
        if (newStatus == BookingStatus.CHECKED_OUT && booking.getCheckedOutAt() == null) {
            booking.setCheckedOutAt(LocalDateTime.now());
        }
        Booking saved = bookingRepository.save(booking);
        recordTransition(saved, oldStatus, newStatus, actor, "Status manually updated by "
                + (actor != null ? actor.getUsername() : "system"));
        return saved;
    }

    private void validateStatusTransition(BookingStatus from, BookingStatus to) {
        // Allow no-op transitions.
        if (from == to) return;
        // Cancelled and rejected are terminal.
        if (from == BookingStatus.CANCELLED || from == BookingStatus.EXPIRED || from == BookingStatus.NO_SHOW) {
            throw new BusinessRuleException("INVALID_STATE",
                    "Không thể chuyển trạng thái từ " + from + " sang " + to);
        }
        // Completed is also terminal.
        if (from == BookingStatus.COMPLETED) {
            throw new BusinessRuleException("INVALID_STATE",
                    "Đơn đã hoàn thành, không thể thay đổi trạng thái");
        }
    }

    private void recordTransition(Booking booking, BookingStatus from, BookingStatus to, User user, String reason) {
        BookingStatusTransition transition = new BookingStatusTransition(booking, from, to, user, reason);
        transitionRepository.save(transition);

        if (to != null && notificationService != null) {
            String title = "Cập nhật đặt phòng";
            String msg = "Trạng thái đặt phòng #" + booking.getId() + " đã chuyển thành " + to.name();
            NotificationType type = NotificationType.BOOKING;
            if (to == BookingStatus.PENDING_PAYMENT) {
                title = "Đặt phòng thành công";
                msg = "Yêu cầu đặt phòng #" + booking.getId() + " đã được ghi nhận. Vui lòng thanh toán.";
            } else if (to == BookingStatus.PAID) {
                title = "Thanh toán thành công";
                msg = "Thanh toán cho đặt phòng #" + booking.getId() + " đã được xác nhận.";
                type = NotificationType.PAYMENT;
                if (notificationProducer != null) {
                    NotificationEvent event = new NotificationEvent("PAYMENT_CUSTOMER", booking.getId());
                    notificationProducer.sendEmailNotification(event);
                }
            } else if (to == BookingStatus.CANCELLED) {
                title = "Đã hủy đặt phòng";
                msg = "Đặt phòng #" + booking.getId() + " đã bị hủy.";
                if (reason != null && !reason.isBlank()) {
                    msg = msg + " Lý do: " + reason;
                }
                if (notificationProducer != null) {
                    NotificationEvent event = new NotificationEvent("BOOKING_CANCELLED", booking.getId());
                    event.setExtraData(reason);
                    notificationProducer.sendEmailNotification(event);
                }
            } else if (to == BookingStatus.EXPIRED) {
                title = "Đặt phòng đã hết hạn";
                msg = "Đặt phòng #" + booking.getId()
                        + " đã hết hạn thanh toán và được hủy tự động.";
                if (notificationProducer != null) {
                    NotificationEvent event = new NotificationEvent("BOOKING_CANCELLED", booking.getId());
                    event.setExtraData("expired");
                    notificationProducer.sendEmailNotification(event);
                }
            } else if (to == BookingStatus.NO_SHOW) {
                title = "Khách không đến";
                msg = "Đặt phòng #" + booking.getId()
                        + " được đánh dấu là không đến nhận phòng.";
                if (notificationProducer != null) {
                    NotificationEvent event = new NotificationEvent("NO_SHOW", booking.getId());
                    notificationProducer.sendEmailNotification(event);
                }
            } else if (to == BookingStatus.COMPLETED) {
                title = "Đặt phòng hoàn thành";
                msg = "Đặt phòng #" + booking.getId() + " đã hoàn thành. Cảm ơn quý khách!";
            }
            notificationService.createNotification(booking.getUser(), title, msg,
                    type.name(),
                    "/account/bookings/" + booking.getId());
        }
    }

    /* ---------------- booking modification ---------------- */

    @Transactional
    public Booking modifyBooking(Integer bookingId, User user, LocalDate newCheckIn, LocalDate newCheckOut, Integer newGuests) {
        Booking booking = loadOrThrow(bookingId);
        if (!booking.getUser().getId().equals(user.getId()) && !"ADMIN".equals(user.getRole())) {
            throw new ForbiddenException("Bạn không có quyền sửa đơn này");
        }
        BookingStatus status = booking.getStatus();
        if (status != BookingStatus.PENDING_PAYMENT && status != BookingStatus.PAID) {
            throw new BusinessRuleException("INVALID_STATE",
                    "Chỉ có thể sửa đơn ở trạng thái: chờ thanh toán hoặc đã thanh toán");
        }
        if (newCheckIn != null && newCheckOut != null) {
            validateDates(newCheckIn, newCheckOut);
            Room room = booking.getRoom();
            // Lock the room row to serialize concurrent booking modifications.
            entityManager.lock(room, LockModeType.PESSIMISTIC_WRITE);
            // Lock the room row + exclude this booking from the conflict set so
            // we don't collide with our own date range.
            List<Booking> conflicts = bookingRepository
                    .findConflictingBookingsForUpdateExcluding(room, bookingId, newCheckIn, newCheckOut);
            if (!conflicts.isEmpty()) {
                throw new BusinessRuleException("ROOM_UNAVAILABLE", "Phòng đã được đặt trong khoảng thời gian mới");
            }
            booking.setCheckInDate(newCheckIn);
            booking.setCheckOutDate(newCheckOut);
            PricingBreakdown pricing = calculatePricing(room, newCheckIn, newCheckOut);
            // Discount was issued against the original subtotal; cap it so the
            // customer is not refunded more than the new stay is worth.
            BigDecimal discount = booking.getDiscountAmount() != null
                    ? booking.getDiscountAmount() : BigDecimal.ZERO;
            if (discount.compareTo(pricing.subtotal()) > 0) {
                discount = pricing.subtotal();
            }
            BigDecimal newTotal = pricing.subtotal()
                    .subtract(discount)
                    .add(pricing.serviceFee())
                    .add(pricing.taxAmount());
            if (newTotal.signum() < 0) {
                newTotal = BigDecimal.ZERO;
            }
            booking.setSubtotalPrice(pricing.subtotal());
            booking.setServiceFee(pricing.serviceFee());
            booking.setTaxAmount(pricing.taxAmount());
            booking.setDiscountAmount(discount);
            booking.setTotalPrice(newTotal);
        }
        if (newGuests != null) {
            if (booking.getRoom().getCapacity() != null && newGuests > booking.getRoom().getCapacity()) {
                throw new BusinessRuleException("GUEST_LIMIT_EXCEEDED",
                        "Số khách vượt quá sức chứa (tối đa " + booking.getRoom().getCapacity() + ")");
            }
            if (newGuests < 1) {
                throw new BusinessRuleException("INVALID_GUEST_COUNT", "Số khách phải >= 1");
            }
            booking.setGuests(newGuests);
        }
        booking.setModifiedAt(LocalDateTime.now());
        Booking saved = bookingRepository.save(booking);
        recordTransition(saved, status, status, user, "Booking modified: dates/guests updated");
        log.info("Booking #{} modified by {}", bookingId, user.getUsername());
        return saved;
    }

    /* ---------------- check-in / check-out ---------------- */

    @Transactional
    public Booking checkIn(Integer bookingId, User adminUser) {
        Booking booking = loadOrThrow(bookingId);
        if (booking.getStatus() != BookingStatus.PAID) {
            throw new BusinessRuleException("INVALID_STATE",
                    "Chỉ có thể nhận phòng cho đơn đã thanh toán (hiện tại: " + booking.getStatus() + ")");
        }
        if (booking.getCheckInDate().isAfter(LocalDate.now())) {
            throw new BusinessRuleException("TOO_EARLY",
                    "Chưa đến ngày nhận phòng");
        }
        BookingStatus oldStatus = booking.getStatus();
        booking.setStatus(BookingStatus.CHECKED_IN);
        booking.setCheckedInAt(LocalDateTime.now());
        Booking saved = bookingRepository.save(booking);
        recordTransition(saved, oldStatus, BookingStatus.CHECKED_IN, adminUser, "Guest checked in");
        log.info("Booking #{} checked in by {}", bookingId, adminUser.getUsername());
        return saved;
    }

    @Transactional
    public Booking checkOut(Integer bookingId, User adminUser) {
        Booking booking = loadOrThrow(bookingId);
        if (booking.getStatus() != BookingStatus.CHECKED_IN) {
            throw new BusinessRuleException("INVALID_STATE",
                    "Chỉ có thể trả phòng cho đơn đã nhận phòng (hiện tại: " + booking.getStatus() + ")");
        }
        BookingStatus oldStatus = booking.getStatus();
        booking.setStatus(BookingStatus.CHECKED_OUT);
        booking.setCheckedOutAt(LocalDateTime.now());
        Booking saved = bookingRepository.save(booking);
        recordTransition(saved, oldStatus, BookingStatus.CHECKED_OUT, adminUser, "Guest checked out");
        log.info("Booking #{} checked out by {}", bookingId, adminUser.getUsername());
        return saved;
    }

    
    @Transactional
    public Booking markAsNoShow(Integer bookingId, User adminUser) {
        Booking booking = loadOrThrow(bookingId);
        if (booking.getStatus() != BookingStatus.PAID) {
            throw new BusinessRuleException("INVALID_STATE", "Chỉ có thể đánh dấu Không đến cho đơn đã thanh toán");
        }
        // The guest was expected on the check-in date; flagging no-show
        // before then does not make sense. Allow it only after the day of
        // arrival has fully passed.
        if (booking.getCheckInDate() == null
                || !LocalDate.now().isAfter(booking.getCheckInDate())) {
            throw new BusinessRuleException("TOO_EARLY",
                    "Chỉ có thể đánh dấu không đến sau ngày nhận phòng");
        }
        BookingStatus oldStatus = booking.getStatus();
        booking.setStatus(BookingStatus.NO_SHOW);
        Booking saved = bookingRepository.save(booking);
        recordTransition(saved, oldStatus, BookingStatus.NO_SHOW, adminUser, "Guest did not arrive");
        log.info("Booking #{} marked as No Show by {}", bookingId, adminUser.getUsername());
        return saved;
    }

    /* ---------------- timeline ---------------- */

    public List<BookingStatusTransition> getBookingTimeline(Integer bookingId) {
        Booking booking = loadOrThrow(bookingId);
        return transitionRepository.findByBookingOrderByCreatedAtAsc(booking);
    }

    /* ---------------- today operations ---------------- */

    public List<Booking> getTodayCheckIns() {
        return bookingRepository.findByCheckInDate(LocalDate.now()).stream()
                .filter(b -> b.getStatus() == BookingStatus.PAID)
                .toList();
    }

    public List<Booking> getTodayCheckOuts() {
        return bookingRepository.findByCheckOutDate(LocalDate.now()).stream()
                .filter(b -> b.getStatus() == BookingStatus.CHECKED_IN || b.getStatus() == BookingStatus.PAID)
                .toList();
    }

    /* ---------------- analytics (admin dashboard) ---------------- */

    /**
     * Returns revenue-per-day between {@code startDate} (inclusive) and
     * {@code endDate} (inclusive). Missing days are filled with zero so the
     * chart always has a contiguous timeline. Revenue is computed from
     * bookings whose status represents realised revenue
     * (PAID/CHECKED_IN/CHECKED_OUT/COMPLETED).
     */
    @Transactional(readOnly = true)
    public java.util.List<java.util.Map<String, Object>> getRevenueTimeseries(LocalDate startDate,
                                                                              LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new BusinessRuleException("INVALID_DATE", "Thiếu startDate/endDate");
        }
        if (endDate.isBefore(startDate)) {
            throw new BusinessRuleException("INVALID_DATE", "endDate phải >= startDate");
        }
        // Cap the window so a runaway date range can't hammer the DB.
        if (ChronoUnit.DAYS.between(startDate, endDate) > 366) {
            throw new BusinessRuleException("RANGE_TOO_LARGE", "Khoảng thời gian tối đa 366 ngày");
        }
        java.util.Map<String, BigDecimal> byDay = new java.util.LinkedHashMap<>();
        for (Object[] row : bookingRepository.aggregateRevenueByDay(startDate, endDate)) {
            java.sql.Date d = (java.sql.Date) row[0];
            BigDecimal rev = (BigDecimal) row[1];
            byDay.put(d.toString(), rev != null ? rev : BigDecimal.ZERO);
        }
        java.util.List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            java.util.Map<String, Object> point = new java.util.LinkedHashMap<>();
            point.put("date", cursor.toString());
            point.put("revenue", byDay.getOrDefault(cursor.toString(), BigDecimal.ZERO));
            result.add(point);
            cursor = cursor.plusDays(1);
        }
        return result;
    }

    /**
     * Returns occupancy rate per day in [startDate, endDate]. {@code
     * occupancyRate} is the percentage (0..100) of rooms occupied on that
     * day, rounded to 1 decimal. Requires the total room count.
     */
    @Transactional(readOnly = true)
    public java.util.List<java.util.Map<String, Object>> getOccupancyTimeseries(LocalDate startDate,
                                                                               LocalDate endDate,
                                                                               int totalRooms) {
        if (startDate == null || endDate == null) {
            throw new BusinessRuleException("INVALID_DATE", "Thiếu startDate/endDate");
        }
        if (endDate.isBefore(startDate)) {
            throw new BusinessRuleException("INVALID_DATE", "endDate phải >= startDate");
        }
        if (ChronoUnit.DAYS.between(startDate, endDate) > 366) {
            throw new BusinessRuleException("RANGE_TOO_LARGE", "Khoảng thời gian tối đa 366 ngày");
        }
        if (totalRooms <= 0) {
            return java.util.List.of();
        }
        java.util.Map<String, java.util.Set<Integer>> occupiedRoomIdsByDay = new java.util.HashMap<>();
        // For each active booking overlapping the window, enumerate every day
        // it blocks and record the room id so we don't double-count a single
        // room multiple times on the same calendar day.
        java.util.List<Booking> overlapping = bookingRepository.findBookingsInDateRange(
                startDate.minusDays(1), endDate.plusDays(1));
        for (Booking b : overlapping) {
            if (!isActiveForOccupancy(b.getStatus())) continue;
            LocalDate from = b.getCheckInDate().isBefore(startDate) ? startDate : b.getCheckInDate();
            LocalDate to = b.getCheckOutDate().isAfter(endDate) ? endDate : b.getCheckOutDate();
            if (from == null || to == null || from.isAfter(to)) continue;
            LocalDate cur = from;
            while (!cur.isAfter(to)) {
                occupiedRoomIdsByDay
                        .computeIfAbsent(cur.toString(), k -> new java.util.HashSet<>())
                        .add(b.getRoom().getId());
                cur = cur.plusDays(1);
            }
        }
        java.util.List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
        LocalDate cursor = startDate;
        BigDecimal roomCount = BigDecimal.valueOf(totalRooms);
        while (!cursor.isAfter(endDate)) {
            int occupied = occupiedRoomIdsByDay
                    .getOrDefault(cursor.toString(), java.util.Set.of()).size();
            BigDecimal rate = BigDecimal.valueOf(occupied)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(roomCount, 1, RoundingMode.HALF_UP);
            java.util.Map<String, Object> point = new java.util.LinkedHashMap<>();
            point.put("date", cursor.toString());
            point.put("occupiedRooms", occupied);
            point.put("totalRooms", totalRooms);
            point.put("occupancyRate", rate);
            result.add(point);
            cursor = cursor.plusDays(1);
        }
        return result;
    }

    private static boolean isActiveForOccupancy(BookingStatus status) {
        return status == BookingStatus.PENDING_PAYMENT
                || status == BookingStatus.PAID
                || status == BookingStatus.CHECKED_IN
                || status == BookingStatus.CHECKED_OUT
                || status == BookingStatus.COMPLETED;
    }

    /** Bookings with a non-zero refund — backs the admin refunds list. */
    @Transactional(readOnly = true)
    public List<Booking> getRefundedBookings() {
        return bookingRepository.findBookingsWithRefunds();
    }

    /**
     * Customer-facing dashboard summary. Aggregates one user's activity
     * without touching unrelated users' data.
     */
    @Transactional(readOnly = true)
    public java.util.Map<String, Object> getCustomerDashboard(User user, int wishlistCount, long unreadNotifications) {
        if (user == null) {
            throw new BusinessRuleException("UNAUTHORIZED", "Cần đăng nhập");
        }
        List<Booking> mine = bookingRepository.findByUserOrderByCreatedAtDesc(user);
        LocalDate today = LocalDate.now();
        int upcoming = 0;
        int current = 0;
        int completed = 0;
        int cancelled = 0;
        BigDecimal totalSpent = BigDecimal.ZERO;
        Booking nextStay = null;
        for (Booking b : mine) {
            switch (b.getStatus()) {
                case COMPLETED, CHECKED_OUT -> completed++;
                case CANCELLED, EXPIRED, NO_SHOW -> cancelled++;
                default -> {
                    if (b.getCheckOutDate() != null && b.getCheckOutDate().isBefore(today)) {
                        completed++;
                    } else if (b.getCheckInDate() != null && !b.getCheckInDate().isAfter(today)
                            && b.getCheckOutDate() != null && b.getCheckOutDate().isAfter(today)) {
                        current++;
                    } else {
                        upcoming++;
                    }
                }
            }
            if (b.getStatus() == BookingStatus.PAID
                    || b.getStatus() == BookingStatus.CHECKED_IN
                    || b.getStatus() == BookingStatus.CHECKED_OUT
                    || b.getStatus() == BookingStatus.COMPLETED) {
                if (b.getTotalPrice() != null) {
                    totalSpent = totalSpent.add(b.getTotalPrice());
                }
            }
            if (nextStay == null && b.getCheckInDate() != null
                    && !b.getCheckInDate().isBefore(today)
                    && (b.getStatus() == BookingStatus.PAID || b.getStatus() == BookingStatus.CHECKED_IN)) {
                nextStay = b;
            }
        }
        java.util.Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("totalBookings", mine.size());
        data.put("upcomingCount", upcoming);
        data.put("currentCount", current);
        data.put("completedCount", completed);
        data.put("cancelledCount", cancelled);
        data.put("wishlistCount", wishlistCount);
        data.put("unreadNotifications", unreadNotifications);
        data.put("totalSpent", totalSpent);
        data.put("nextStay", nextStay);
        return data;
    }

    /* ---------------- helpers ---------------- */

    private Booking loadOrThrow(Integer id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", id));
    }

    private void validateDates(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null) {
            throw new BusinessRuleException("INVALID_DATE", "Thiếu ngày nhận/trả phòng");
        }
        if (checkIn.isBefore(LocalDate.now())) {
            throw new BusinessRuleException("INVALID_DATE", "Ngày check-in không được trước hôm nay");
        }
        if (checkOut.isBefore(checkIn)) {
            throw new BusinessRuleException("INVALID_DATE",
                    "Ngày trả phòng phải bằng hoặc sau ngày nhận phòng");
        }
    }

    private void calculateRefund(Booking booking) {
        if (booking.getTotalPrice() == null
                || booking.getTotalPrice().signum() <= 0
                || booking.getCheckInDate() == null) {
            booking.setRefundPercentage(0);
            booking.setRefundAmount(BigDecimal.ZERO);
            return;
        }
        LocalDate today = LocalDate.now();
        long daysUntilCheckIn = ChronoUnit.DAYS.between(today, booking.getCheckInDate());
        int refundPercentage = 0;
        // Once the check-in date has arrived (or passed) the stay is treated as
        // consumed — no refund is owed even if the user cancels before check-out.
        if (daysUntilCheckIn < 0) {
            refundPercentage = 0;
        } else if (daysUntilCheckIn >= FULL_REFUND_DAYS) {
            refundPercentage = FULL_REFUND_PERCENT;
        } else if (daysUntilCheckIn >= PARTIAL_REFUND_DAYS) {
            refundPercentage = PARTIAL_REFUND_PERCENT;
        }
        BigDecimal refundAmount = booking.getTotalPrice()
                .multiply(BigDecimal.valueOf(refundPercentage))
                .divide(PERCENT_DIVISOR, 0, RoundingMode.HALF_UP);
        booking.setRefundPercentage(refundPercentage);
        booking.setRefundAmount(refundAmount);
    }
}
