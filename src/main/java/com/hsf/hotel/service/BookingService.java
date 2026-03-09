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
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt phòng"));
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
     * 2. Tạo booking với status CONFIRMED
     * 3. Gửi email thông báo Admin
     */
    @Transactional
    public Booking createBooking(User user, Room room, LocalDate checkIn, LocalDate checkOut,
            String guestName, String guestPhone, String notes) {
        // Step 1: Check availability
        if (!isRoomAvailable(room, checkIn, checkOut)) {
            throw new RuntimeException("Phòng đã được đặt trong khoảng thời gian này");
        }

        // Step 2: Create booking with CONFIRMED status
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setRoom(room);
        booking.setCheckInDate(checkIn);
        booking.setCheckOutDate(checkOut);
        booking.setGuestName(guestName);
        booking.setGuestPhone(guestPhone);
        booking.setNotes(notes);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setTotalPrice(calculateTotalPrice(room, checkIn, checkOut));

        Booking savedBooking = bookingRepository.save(booking);

        // Step 3: Notify admin
        System.out.println("📧 New booking #" + savedBooking.getId() + " - Notifying admin");
        emailService.sendNewBookingNotificationToAdmin(savedBooking, adminEmail);

        return savedBooking;
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
