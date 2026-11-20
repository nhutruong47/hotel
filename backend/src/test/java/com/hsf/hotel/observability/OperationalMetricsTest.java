package com.hsf.hotel.observability;

import com.hsf.hotel.booking.model.BookingStatus;
import com.hsf.hotel.booking.repository.BookingRepository;
import com.hsf.hotel.notification.model.NotificationOutbox;
import com.hsf.hotel.notification.repository.NotificationOutboxRepository;
import com.hsf.hotel.payment.model.PaymentRefund;
import com.hsf.hotel.payment.repository.PaymentRefundRepository;
import com.hsf.hotel.payment.repository.PaymentRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperationalMetricsTest {

    @Mock private NotificationOutboxRepository outbox;
    @Mock private PaymentRepository payments;
    @Mock private PaymentRefundRepository refunds;
    @Mock private BookingRepository bookings;

    @Test
    void exposesBusinessGauges() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        new OperationalMetrics(registry, outbox, payments, refunds, bookings);
        when(outbox.countByStatus(NotificationOutbox.OutboxStatus.PENDING)).thenReturn(4L);
        when(outbox.countByStatus(NotificationOutbox.OutboxStatus.FAILED)).thenReturn(1L);
        when(outbox.findOldestCreatedAt(NotificationOutbox.OutboxStatus.PENDING))
                .thenReturn(LocalDateTime.now().minusMinutes(12));
        when(payments.countByReviewRequiredTrue()).thenReturn(2L);
        when(refunds.countByStatus(PaymentRefund.RefundStatus.PENDING_MANUAL)).thenReturn(3L);
        when(bookings.countByStatus(BookingStatus.PENDING_PAYMENT)).thenReturn(5L);

        assertThat(registry.get("hotel.outbox.events").tag("status", "pending").gauge().value()).isEqualTo(4d);
        assertThat(registry.get("hotel.outbox.events").tag("status", "failed").gauge().value()).isEqualTo(1d);
        assertThat(registry.get("hotel.outbox.oldest.pending.age").gauge().value()).isBetween(700d, 740d);
        assertThat(registry.get("hotel.payments.review.required").gauge().value()).isEqualTo(2d);
        assertThat(registry.get("hotel.refunds.pending.manual").gauge().value()).isEqualTo(3d);
        assertThat(registry.get("hotel.bookings.pending.payment").gauge().value()).isEqualTo(5d);
    }

    @Test
    void failingQueryReportsNaNInsteadOfBreakingTheScrape() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        new OperationalMetrics(registry, outbox, payments, refunds, bookings);
        when(payments.countByReviewRequiredTrue()).thenThrow(new IllegalStateException("db down"));

        assertThat(registry.get("hotel.payments.review.required").gauge().value()).isNaN();
    }

    @Test
    void noPendingEventsMeansZeroAge() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        new OperationalMetrics(registry, outbox, payments, refunds, bookings);
        when(outbox.findOldestCreatedAt(NotificationOutbox.OutboxStatus.PENDING)).thenReturn(null);

        assertThat(registry.get("hotel.outbox.oldest.pending.age").gauge().value()).isZero();
    }
}
