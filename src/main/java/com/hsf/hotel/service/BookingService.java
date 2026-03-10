package com.hsf.hotel.service;

import com.hsf.hotel.model.*;
import com.hsf.hotel.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private EmailService emailService;

    @Value("${app.booking.payment-deadline-hours:24}")
    private int paymentDeadlineHours;

    @Value("${app.admin.email:admin@hotel.com}")
    private String adminEmail;

    public List<Booking> getUserBookings(User user) {
        return bookingRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public Optional<Booking> getBookingById(Integer id) {
        return bookingRepository.findById(id);
    }

    // Admin methods
    public List<Booking> getAllBookings() {
        return bookingRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Booking> getTodayBookings() {
        LocalDate today = LocalDate.now();
        return bookingRepository.findByCheckInDate(today);
    }

    public List<Booking> getBookingsByStatus(BookingStatus status) {
        return bookingRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    public List<Object[]> getMostBookedRooms() {
        return bookingRepository.findMostBookedRooms();
    }

    public List<Object[]> getMostRatingRooms() {
        return bookingRepository.findMostratingRooms();
    }

    public BigDecimal getMonthlyRevenue() {
        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate endOfMonth = startOfMonth.plusMonths(1); // Exclude end of month for proper range
        List<Booking> monthlyBookings = bookingRepository.findSuccessfulBookingsInDateRange(startOfMonth, endOfMonth);

        return monthlyBookings.stream()
                .map(Booking::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional
    public Booking updateBookingStatus(Integer bookingId, BookingStatus newStatus) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        booking.setStatus(newStatus);

        if (newStatus == BookingStatus.CANCELLED) {
            calculateRefund(booking);
        }

        return bookingRepository.save(booking);
    }

    public BigDecimal calculateTotalPrice(Room room, LocalDate checkIn, LocalDate checkOut) {
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        if (nights == 0) {
            // Đặt phòng và trả phòng trong cùng 1 ngày (trong ngày/theo giờ): Tính 50% giá
            // phòng
            return room.getPricePerNight().multiply(new BigDecimal("0.5"));
        }
        return room.getPricePerNight().multiply(BigDecimal.valueOf(nights));
    }

    public boolean isRoomAvailable(Room room, LocalDate checkIn, LocalDate checkOut) {
        if (!room.getIsAvailable()) {
            return false;
        }
        return bookingRepository.findConflictingBookings(room, checkIn, checkOut).isEmpty();
    }

    /**
     * WORKFLOW: Tạo booking mới
     * 1. Validate phòng available
     * 2. Tạo booking với status AWAITING_PAYMENT và set payment deadline
     * 3. Gửi email thông báo Admin
     */
    @Transactional
    public Booking createBooking(User user, Room room, LocalDate checkIn, LocalDate checkOut,
            String guestName, String guestPhone, String notes) {
        // Step 1: Check availability
        if (!isRoomAvailable(room, checkIn, checkOut)) {
            throw new RuntimeException("Room is already booked during this time");
        }

        // Step 2: Create booking with AWAITING_PAYMENT status
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setRoom(room);
        booking.setCheckInDate(checkIn);
        booking.setCheckOutDate(checkOut);
        booking.setGuestName(guestName);
        booking.setGuestPhone(guestPhone);
        booking.setNotes(notes);
        booking.setStatus(BookingStatus.AWAITING_PAYMENT);
        booking.setPaymentDeadline(LocalDateTime.now().plusHours(paymentDeadlineHours));
        booking.setTotalPrice(calculateTotalPrice(room, checkIn, checkOut));

        Booking savedBooking = bookingRepository.save(booking);

        // Step 3: Notify admin
        System.out.println("📧 New booking #" + savedBooking.getId() + " - Notifying admin");
        emailService.sendNewBookingNotificationToAdmin(savedBooking, adminEmail);

        return savedBooking;
    }

    /**
     * Save booking entity (useful to persist changes like discount/voucher)
     */
    @Transactional
    public Booking saveBooking(Booking booking) {
        return bookingRepository.save(booking);
    }

    /**
     * WORKFLOW: Admin duyệt booking
     * 1. Kiểm tra booking tồn tại và đang PENDING
     * 2. Set status = AWAITING_PAYMENT
     * 3. Set payment deadline
     * 4. Gửi email thông báo user
     */
    @Transactional
    public Booking approveBooking(Integer bookingId, User adminUser) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new RuntimeException("Only pending bookings can be approved");
        }

        // Update booking
        booking.setStatus(BookingStatus.AWAITING_PAYMENT);
        booking.setApprovedBy(adminUser);
        booking.setApprovedAt(LocalDateTime.now());
        booking.setPaymentDeadline(LocalDateTime.now().plusHours(paymentDeadlineHours));

        Booking savedBooking = bookingRepository.save(booking);

        // Send email to user
        System.out.println("✅ Booking #" + bookingId + " approved by " + adminUser.getUsername());
        emailService.sendBookingApprovedEmail(savedBooking);

        return savedBooking;
    }

    /**
     * WORKFLOW: Admin từ chối booking
     * 1. Kiểm tra booking tồn tại và đang PENDING
     * 2. Set status = REJECTED
     * 3. Lưu lý do từ chối
     * 4. Gửi email thông báo user
     */
    @Transactional
    public Booking rejectBooking(Integer bookingId, User adminUser, String reason) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new RuntimeException("Only pending bookings can be rejected");
        }

        // Update booking
        booking.setStatus(BookingStatus.REJECTED);
        booking.setApprovedBy(adminUser);
        booking.setApprovedAt(LocalDateTime.now());
        booking.setRejectionReason(reason != null ? reason : "Not qualified");

        Booking savedBooking = bookingRepository.save(booking);

        // Send email to user
        System.out.println("❌ Booking #" + bookingId + " rejected by " + adminUser.getUsername());
        emailService.sendBookingRejectedEmail(savedBooking);

        return savedBooking;
    }

    /**
     * WORKFLOW: Xác nhận thanh toán
     * 1. Update status = CONFIRMED
     * 2. Set paidAt timestamp
     * 3. Gửi email thông báo Admin
     */
    @Transactional
    public Booking confirmPayment(Integer bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (booking.getStatus() != BookingStatus.AWAITING_PAYMENT) {
            throw new RuntimeException("Booking is not in awaiting payment status");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPaidAt(LocalDateTime.now());

        Booking savedBooking = bookingRepository.save(booking);

        System.out.println("💰 Payment confirmed for booking #" + bookingId + " at " + booking.getPaidAt());

        // Notify admin about payment
        emailService.sendPaymentReceivedNotificationToAdmin(savedBooking, adminEmail);

        return savedBooking;
    }

    /**
     * Lấy các booking quá hạn thanh toán (dùng cho scheduler)
     */
    public List<Booking> getExpiredPaymentBookings() {
        return bookingRepository.findExpiredPaymentBookings(LocalDateTime.now());
    }

    @Transactional
    public Booking cancelBooking(Integer bookingId, User user) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        // Check ownership
        if (!booking.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("You do not have permission to cancel this booking");
        }

        // Check if can cancel
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new RuntimeException("Booking has already been cancelled");
        }
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new RuntimeException("Cannot cancel a completed booking");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        calculateRefund(booking);
        return bookingRepository.save(booking);
    }

    private void calculateRefund(Booking booking) {
        if (booking.getRefundAmount() == null) {
            long daysUntilCheckIn = ChronoUnit.DAYS.between(LocalDate.now(), booking.getCheckInDate());
            int refundPercentage = 0;

            if (daysUntilCheckIn >= 3) {
                refundPercentage = 100;
            } else if (daysUntilCheckIn >= 1) {
                refundPercentage = 50;
            }

            BigDecimal refundAmount = booking.getTotalPrice()
                    .multiply(BigDecimal.valueOf(refundPercentage))
                    .divide(BigDecimal.valueOf(100));

            booking.setRefundPercentage(refundPercentage);
            booking.setRefundAmount(refundAmount);
        }
    }
}
