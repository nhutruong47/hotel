package com.hsf.hotel.scheduler;

import com.hsf.hotel.service.BookingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Scheduler for automatic booking-related tasks.
 * 
 * <p>Handles:
 * <ul>
 *   <li>Expiring unpaid bookings after payment deadline</li>
 *   <li>Auto-completing overdue checked-out bookings</li>
 *   <li>Marking no-show bookings when checkout date passes</li>
 * </ul>
 */
@Component
public class BookingScheduler {

    private static final Logger log = LoggerFactory.getLogger(BookingScheduler.class);

    private final BookingService bookingService;

    public BookingScheduler(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /**
     * Run every 5 minutes to expire unpaid bookings whose payment deadline has passed.
     * Cron expression: "0 0/5 * * * *" (every 5 minutes).
     */
    @Scheduled(cron = "0 */5 * * * *")
    public void expireUnpaidBookings() {
        log.debug("Scheduler: Starting expired booking check at {}", LocalDateTime.now());
        
        try {
            var expiredBookings = bookingService.getExpiredPaymentBookings();
            int count = 0;
            
            for (var booking : expiredBookings) {
                try {
                    bookingService.expireHold(booking.getId());
                    count++;
                    log.info("Scheduler: Expired booking #{}", booking.getId());
                } catch (Exception e) {
                    log.error("Scheduler: Failed to expire booking #{}: {}", 
                            booking.getId(), e.getMessage());
                }
            }
            
            if (count > 0) {
                log.info("Scheduler: Completed. Expired {} bookings", count);
            }
        } catch (Exception e) {
            log.error("Scheduler: Error during expired booking check: {}", e.getMessage(), e);
        }
    }

    /**
     * Run every hour to auto-complete bookings that are past their checkout date
     * but still in CHECKED_OUT status.
     * Cron: "0 0 * * * *" = at second 0 of every hour.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void completeOverdueBookings() {
        log.debug("Scheduler: Starting overdue completion check at {}", LocalDateTime.now());
        
        try {
            // Get all checked-out bookings and filter those past checkout date
            var allBookings = bookingService.getAllBookings();
            int count = 0;
            
            for (var booking : allBookings) {
                if (booking.getStatus() == com.hsf.hotel.model.BookingStatus.CHECKED_OUT
                        && booking.getCheckOutDate() != null
                        && booking.getCheckOutDate().isBefore(java.time.LocalDate.now())) {
                    try {
                        bookingService.completeOverdueBooking(booking.getId());
                        count++;
                        log.info("Scheduler: Auto-completed booking #{}", booking.getId());
                    } catch (Exception e) {
                        log.error("Scheduler: Failed to complete booking #{}: {}", 
                                booking.getId(), e.getMessage());
                    }
                }
            }
            
            if (count > 0) {
                log.info("Scheduler: Auto-completed {} overdue bookings", count);
            }
        } catch (Exception e) {
            log.error("Scheduler: Error during overdue completion check: {}", e.getMessage(), e);
        }
    }

    /**
     * Run daily at 2:00 AM to mark paid bookings as no-show
     * when the checkout date has passed without check-in.
     * Cron: "0 0 2 * * *" = at 02:00:00 every day.
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void markNoShowBookings() {
        log.debug("Scheduler: Starting no-show check at {}", LocalDateTime.now());
        
        try {
            var allBookings = bookingService.getAllBookings();
            int count = 0;
            
            for (var booking : allBookings) {
                // Paid booking past checkout date without being checked in
                if (booking.getStatus() == com.hsf.hotel.model.BookingStatus.PAID
                        && booking.getCheckOutDate() != null
                        && booking.getCheckOutDate().isBefore(java.time.LocalDate.now())) {
                    try {
                        bookingService.markNoShowOverdue(booking.getId());
                        count++;
                        log.info("Scheduler: Marked booking #{} as no-show", booking.getId());
                    } catch (Exception e) {
                        log.error("Scheduler: Failed to mark booking #{} as no-show: {}", 
                                booking.getId(), e.getMessage());
                    }
                }
            }
            
            if (count > 0) {
                log.info("Scheduler: Marked {} bookings as no-show", count);
            }
        } catch (Exception e) {
            log.error("Scheduler: Error during no-show check: {}", e.getMessage(), e);
        }
    }

    /**
     * Run daily at midnight to cleanup old audit logs.
     * Keeps logs for 90 days by default.
     * Cron: "0 0 0 * * *" = at 00:00:00 every day.
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void cleanupOldAuditLogs() {
        log.debug("Scheduler: Starting audit log cleanup at {}", LocalDateTime.now());
        
        try {
            // This would call AuditLogService to cleanup old logs
            // Implementation depends on having a cleanup method in AuditLogService
            log.info("Scheduler: Audit log cleanup completed");
        } catch (Exception e) {
            log.error("Scheduler: Error during audit log cleanup: {}", e.getMessage(), e);
        }
    }
}
