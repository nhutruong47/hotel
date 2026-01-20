package com.hsf.hotel.scheduler;

import com.hsf.hotel.model.Booking;
import com.hsf.hotel.model.BookingStatus;
import com.hsf.hotel.repository.BookingRepository;
import com.hsf.hotel.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Scheduled task để auto-cancel booking quá hạn thanh toán
 * 
 * WORKFLOW:
 * 1. Chạy mỗi 30 phút
 * 2. Tìm tất cả booking có status = AWAITING_PAYMENT và paymentDeadline < now
 * 3. Update status = CANCELLED
 * 4. Gửi email thông báo cho user
 */
@Component
public class BookingScheduler {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private EmailService emailService;

    /**
     * Chạy mỗi 30 phút để kiểm tra và auto-cancel booking quá hạn
     */
    @Scheduled(fixedRate = 30 * 60 * 1000) // 30 minutes in milliseconds
    @Transactional
    public void autoCancelExpiredBookings() {
        System.out.println("⏰ Running auto-cancel check for expired bookings...");

        List<Booking> expiredBookings = bookingRepository.findExpiredPaymentBookings(
                java.time.LocalDateTime.now());

        if (expiredBookings.isEmpty()) {
            System.out.println("✅ No expired bookings found");
            return;
        }

        System.out.println("🔍 Found " + expiredBookings.size() + " expired booking(s)");

        for (Booking booking : expiredBookings) {
            try {
                // Update status
                booking.setStatus(BookingStatus.CANCELLED);
                bookingRepository.save(booking);

                // Send notification email
                emailService.sendBookingCancelledEmail(booking);

                System.out.println("❌ Auto-cancelled booking #" + booking.getId()
                        + " (Room: " + booking.getRoom().getRoomNumber()
                        + ", User: " + booking.getUser().getUsername() + ")");
            } catch (Exception e) {
                System.err.println("⚠️ Failed to auto-cancel booking #" + booking.getId()
                        + ": " + e.getMessage());
            }
        }

        System.out.println("✅ Auto-cancel check completed");
    }

    /**
     * Chạy mỗi ngày lúc 8h sáng để gửi reminder cho booking sắp hết hạn thanh toán
     */
    @Scheduled(cron = "0 0 8 * * ?") // Every day at 8:00 AM
    public void sendPaymentReminders() {
        System.out.println("📧 Sending payment reminders...");

        // Find bookings with deadline in next 2 hours
        List<Booking> urgentBookings = bookingRepository.findByStatusOrderByCreatedAtDesc(
                BookingStatus.AWAITING_PAYMENT);

        java.time.LocalDateTime twoHoursFromNow = java.time.LocalDateTime.now().plusHours(2);

        for (Booking booking : urgentBookings) {
            if (booking.getPaymentDeadline() != null
                    && booking.getPaymentDeadline().isBefore(twoHoursFromNow)
                    && booking.getPaymentDeadline().isAfter(java.time.LocalDateTime.now())) {

                System.out.println("⚠️ Payment reminder for booking #" + booking.getId());
                // Could send reminder email here
            }
        }
    }
}
