package com.hsf.hotel.service;

import com.hsf.hotel.model.*;
import com.hsf.hotel.repository.BookingRepository;
import com.hsf.hotel.repository.RoomRepository;
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
    private RoomRepository roomRepository;

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

    public List<Booking> getPendingBookings() {
        return bookingRepository.findByStatusOrderByCreatedAtDesc(BookingStatus.PENDING);
    }

    public long getPendingBookingsCount() {
        return bookingRepository.countByStatus(BookingStatus.PENDING);
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
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt phòng"));
        booking.setStatus(newStatus);
        return bookingRepository.save(booking);
    }

    public BigDecimal calculateTotalPrice(Room room, LocalDate checkIn, LocalDate checkOut) {
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
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
     * 2. Tạo booking với status PENDING
     * 3. Gửi email thông báo Admin
     */
    @Transactional
    public Booking createBooking(User user, Room room, LocalDate checkIn, LocalDate checkOut,
            String guestName, String guestPhone, String notes) {
        // Step 1: Check availability
        if (!isRoomAvailable(room, checkIn, checkOut)) {
            throw new RuntimeException("Phòng đã được đặt trong khoảng thời gian này");
        }

        // Step 2: Create booking with PENDING status
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setRoom(room);
        booking.setCheckInDate(checkIn);
        booking.setCheckOutDate(checkOut);
        booking.setGuestName(guestName);
        booking.setGuestPhone(guestPhone);
        booking.setNotes(notes);
        booking.setStatus(BookingStatus.PENDING); // Changed from CONFIRMED to PENDING
        booking.setTotalPrice(calculateTotalPrice(room, checkIn, checkOut));

        Booking savedBooking = bookingRepository.save(booking);

        // Step 3: Notify admin
        System.out.println("📧 New booking #" + savedBooking.getId() + " - Notifying admin");
        emailService.sendNewBookingNotificationToAdmin(savedBooking, adminEmail);

        return savedBooking;
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
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt phòng"));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new RuntimeException("Chỉ có thể duyệt đơn đang chờ xác nhận");
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
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt phòng"));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new RuntimeException("Chỉ có thể từ chối đơn đang chờ xác nhận");
        }

        // Update booking
        booking.setStatus(BookingStatus.REJECTED);
        booking.setApprovedBy(adminUser);
        booking.setApprovedAt(LocalDateTime.now());
        booking.setRejectionReason(reason != null ? reason : "Không đủ điều kiện");

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
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt phòng"));

        if (booking.getStatus() != BookingStatus.AWAITING_PAYMENT) {
            throw new RuntimeException("Đơn không ở trạng thái chờ thanh toán");
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
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt phòng"));

        // Check ownership
        if (!booking.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Bạn không có quyền hủy đơn này");
        }

        // Check if can cancel
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new RuntimeException("Đơn đặt phòng đã được hủy trước đó");
        }
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new RuntimeException("Không thể hủy đơn đã hoàn thành");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        return bookingRepository.save(booking);
    }
}
