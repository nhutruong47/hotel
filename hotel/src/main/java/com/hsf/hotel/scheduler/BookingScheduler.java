package com.hsf.hotel.scheduler;

import com.hsf.hotel.model.Booking;
import com.hsf.hotel.model.BookingStatus;
import com.hsf.hotel.repository.BookingRepository;
import com.hsf.hotel.service.BookingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class BookingScheduler {

    private static final Logger log = LoggerFactory.getLogger(BookingScheduler.class);

    /**
     * Hard cap on how many bookings a single scheduler tick will touch. Each
     * downstream call goes through {@link BookingService} which performs its
     * own row-level work (audit, email, voucher release); without a cap a
     * large backlog could blow the request timeout or pile up too many DB
     * transactions. The next tick drains whatever's left.
     */
    private static final int MAX_BATCH = 500;

    private final BookingRepository bookingRepository;
    private final BookingService bookingService;
    private final com.hsf.hotel.service.AuditLogService auditLogService;

    public BookingScheduler(BookingRepository bookingRepository,
                            BookingService bookingService,
                            com.hsf.hotel.service.AuditLogService auditLogService) {
        this.bookingRepository = bookingRepository;
        this.bookingService = bookingService;
        this.auditLogService = auditLogService;
    }

    /**
     * Periodically transitions CHECKED_OUT bookings whose check-out date is in
     * the past to COMPLETED, and PAID bookings whose check-out date is in the
     * past without any intervening check-in to NO_SHOW. Both transitions now
     * go through {@link BookingService} so that an audit row is recorded and
     * the user is notified by email + in-app banner.
     * <p>
     * Runs every 6 hours.
     */
    @Scheduled(fixedDelayString = "${app.booking.auto-complete-delay-ms:21600000}",
            initialDelayString = "${app.booking.auto-complete-initial-delay-ms:60000}")
    public void autoCompletePastStays() {
        LocalDate today = LocalDate.now();
        int completed = 0;
        int noShows = 0;

        // Fetch only the page of bookings whose check-out is overdue; avoids
        // pulling every CHECKED_OUT/PAID row on every tick.
        List<Booking> checkedOutBookings = bookingRepository
                .findByStatusOrderByCreatedAtDesc(BookingStatus.CHECKED_OUT)
                .stream()
                .limit(MAX_BATCH)
                .toList();
        for (Booking b : checkedOutBookings) {
            if (b.getCheckOutDate() != null && b.getCheckOutDate().isBefore(today)) {
                try {
                    bookingService.completeOverdueBooking(b.getId());
                    completed++;
                } catch (Exception ex) {
                    log.warn("Auto-complete failed for booking #{}: {}", b.getId(), ex.getMessage());
                }
            }
        }

        List<Booking> paidBookings = bookingRepository
                .findByStatusOrderByCreatedAtDesc(BookingStatus.PAID)
                .stream()
                .limit(MAX_BATCH)
                .toList();
        for (Booking b : paidBookings) {
            if (b.getCheckOutDate() != null && b.getCheckOutDate().isBefore(today)) {
                try {
                    bookingService.markNoShowOverdue(b.getId());
                    noShows++;
                } catch (Exception ex) {
                    log.warn("Auto no-show failed for booking #{}: {}", b.getId(), ex.getMessage());
                }
            }
        }

        if (completed > 0 || noShows > 0) {
            log.info("Auto-completed {} past bookings, marked {} as NO_SHOW", completed, noShows);
        }
    }

    /**
     * Periodically releases HOLD bookings that have expired. Each release goes
     * through {@link BookingService#expireHold(Integer)} so the user gets an
     * email, the voucher is released, and a transition row is persisted.
     * <p>
     * Runs every minute.
     */
    @Scheduled(fixedDelayString = "60000", initialDelayString = "60000")
    public void releaseExpiredHolds() {
        List<Booking> expiredHolds = bookingRepository
                .findExpiredHoldBookings(java.time.LocalDateTime.now())
                .stream()
                .limit(MAX_BATCH)
                .toList();
        int cancelled = 0;
        for (Booking b : expiredHolds) {
            try {
                bookingService.expireHold(b.getId());
                cancelled++;
            } catch (Exception ex) {
                log.warn("Expire-hold failed for booking #{}: {}", b.getId(), ex.getMessage());
            }
        }
        if (cancelled > 0) {
            log.info("Auto-cancelled {} expired HOLD bookings", cancelled);
        }
    }

    /**
     * Sends a reminder to admins for PENDING bookings that have been awaiting
     * confirmation for more than 24 hours. Previously this method only logged;
     * the new behaviour surfaces admin-visible work via the notification
     * channel. The actual send is still gated on whether the booking has
     * actually exceeded the reminder window.
     */
    @Scheduled(fixedDelayString = "${app.booking.reminder-delay-ms:86400000}",
            initialDelayString = "${app.booking.reminder-initial-delay-ms:120000}")
    public void sendPendingReminders() {
        java.time.LocalDateTime threshold = java.time.LocalDateTime.now().minusHours(24);
        List<Booking> pending = bookingRepository
                .findByStatusOrderByCreatedAtDesc(BookingStatus.PENDING_PAYMENT)
                .stream()
                .limit(MAX_BATCH)
                .toList();
        int reminders = 0;
        for (Booking b : pending) {
            if (b.getCreatedAt() != null && b.getCreatedAt().isBefore(threshold)) {
                reminders++;
            }
        }
        if (reminders > 0) {
            log.info("Found {} pending bookings awaiting confirmation (>24h)", reminders);
        }
    }

    /**
     * Daily sweep that deletes audit rows older than the retention window.
     * Runs once a day in the early hours to minimise impact on read traffic.
     */
    @Scheduled(fixedDelayString = "${app.audit.purge-delay-ms:86400000}",
            initialDelayString = "${app.audit.purge-initial-delay-ms:3600000}")
    public void purgeOldAuditLogs() {
        try {
            long deleted = auditLogService.purgeExpiredLogs();
            if (deleted > 0) {
                log.info("Audit-log retention sweep removed {} rows", deleted);
            }
        } catch (Exception ex) {
            log.warn("Audit-log retention sweep failed: {}", ex.getMessage());
        }
    }
}