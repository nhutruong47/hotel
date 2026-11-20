package com.hsf.hotel.observability;

import com.hsf.hotel.booking.model.BookingStatus;
import com.hsf.hotel.booking.repository.BookingRepository;
import com.hsf.hotel.notification.model.NotificationOutbox;
import com.hsf.hotel.notification.repository.NotificationOutboxRepository;
import com.hsf.hotel.payment.model.PaymentRefund;
import com.hsf.hotel.payment.repository.PaymentRefundRepository;
import com.hsf.hotel.payment.repository.PaymentRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.function.Supplier;

/**
 * Business health gauges for Prometheus (P5, ops/prometheus/alerts.yml). Each gauge
 * runs one cheap COUNT/MIN query per scrape; failures report NaN instead of
 * breaking the scrape. Exposed as e.g. {@code hotel_outbox_events{status="pending"}}.
 */
@Component
// prod (and test) run with spring.main.lazy-initialization=true: without this the
// bean is never instantiated and the gauges silently never appear.
@org.springframework.context.annotation.Lazy(false)
public class OperationalMetrics {

    private static final Logger log = LoggerFactory.getLogger(OperationalMetrics.class);

    private final NotificationOutboxRepository outboxRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentRefundRepository refundRepository;
    private final BookingRepository bookingRepository;

    public OperationalMetrics(MeterRegistry registry,
                              NotificationOutboxRepository outboxRepository,
                              PaymentRepository paymentRepository,
                              PaymentRefundRepository refundRepository,
                              BookingRepository bookingRepository) {
        this.outboxRepository = outboxRepository;
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
        this.bookingRepository = bookingRepository;

        Gauge.builder("hotel.outbox.events", this,
                        m -> m.safe(() -> outboxRepository.countByStatus(NotificationOutbox.OutboxStatus.PENDING)))
                .tag("status", "pending")
                .description("Notification outbox events waiting to be published")
                .register(registry);
        Gauge.builder("hotel.outbox.events", this,
                        m -> m.safe(() -> outboxRepository.countByStatus(NotificationOutbox.OutboxStatus.FAILED)))
                .tag("status", "failed")
                .description("Outbox events that exhausted their retries (dead letters)")
                .register(registry);
        Gauge.builder("hotel.outbox.oldest.pending.age", this, OperationalMetrics::oldestPendingAgeSeconds)
                .baseUnit("seconds")
                .description("Age of the oldest pending outbox event (0 when none)")
                .register(registry);
        Gauge.builder("hotel.payments.review.required", this,
                        m -> m.safe(paymentRepository::countByReviewRequiredTrue))
                .description("Received payments flagged for reconciliation (ADR-0009 Q4)")
                .register(registry);
        Gauge.builder("hotel.refunds.pending.manual", this,
                        m -> m.safe(() -> refundRepository.countByStatus(PaymentRefund.RefundStatus.PENDING_MANUAL)))
                .description("Manual refunds waiting for a bank transfer (ADR-0009 Q5)")
                .register(registry);
        Gauge.builder("hotel.bookings.pending.payment", this,
                        m -> m.safe(() -> bookingRepository.countByStatus(BookingStatus.PENDING_PAYMENT)))
                .description("Bookings holding inventory while awaiting payment")
                .register(registry);
    }

    double oldestPendingAgeSeconds() {
        return safe(() -> {
            LocalDateTime oldest = outboxRepository.findOldestCreatedAt(NotificationOutbox.OutboxStatus.PENDING);
            return oldest == null ? 0L : Math.max(0L, Duration.between(oldest, LocalDateTime.now()).getSeconds());
        });
    }

    private double safe(Supplier<? extends Number> query) {
        try {
            Number value = query.get();
            return value == null ? 0d : value.doubleValue();
        } catch (RuntimeException ex) {
            log.debug("Metric query failed: {}", ex.getMessage());
            return Double.NaN;
        }
    }
}
