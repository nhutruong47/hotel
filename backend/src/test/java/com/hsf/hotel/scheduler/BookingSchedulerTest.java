package com.hsf.hotel.scheduler;

import com.hsf.hotel.admin.service.AuditLogService;
import com.hsf.hotel.booking.model.Booking;
import com.hsf.hotel.booking.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingSchedulerTest {

    @Mock
    private BookingService bookingService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private BookingScheduler bookingScheduler;

    private Booking expiredBooking;

    @BeforeEach
    void setUp() {
        expiredBooking = new Booking();
        expiredBooking.setId(101);
    }

    @Test
    @DisplayName("Should expire unpaid bookings past payment deadline")
    void testExpireUnpaidBookings() {
        when(bookingService.getExpiredPaymentBookings()).thenReturn(List.of(expiredBooking));

        bookingScheduler.expireUnpaidBookings();

        verify(bookingService).expireHold(101);
    }

    @Test
    @DisplayName("Should purge expired audit logs on daily cleanup run")
    void testCleanupOldAuditLogs() {
        when(auditLogService.purgeExpiredLogs()).thenReturn(25L);

        bookingScheduler.cleanupOldAuditLogs();

        verify(auditLogService, times(1)).purgeExpiredLogs();
    }
}
