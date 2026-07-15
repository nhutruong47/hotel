package com.hsf.hotel.service;

import com.hsf.hotel.exception.BusinessRuleException;
import com.hsf.hotel.exception.ResourceNotFoundException;
import com.hsf.hotel.model.Booking;
import com.hsf.hotel.model.BookingStatus;
import com.hsf.hotel.model.Payment;
import com.hsf.hotel.model.User;
import com.hsf.hotel.repository.BookingRepository;
import com.hsf.hotel.repository.PaymentRepository;
import com.hsf.hotel.model.BookingStatusTransition;
import com.hsf.hotel.repository.BookingStatusTransitionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.hsf.hotel.service.payment.PaymentGateway;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final BookingStatusTransitionRepository transitionRepository;
    private final PaymentGateway paymentGateway;
    private final EmailService emailService;
    private final String adminEmail;

    public PaymentService(PaymentRepository paymentRepository,
                          BookingRepository bookingRepository,
                          BookingStatusTransitionRepository transitionRepository,
                          PaymentGateway paymentGateway,
                          EmailService emailService,
                          @Value("${app.admin.email:admin@hotel.com}") String adminEmail) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.transitionRepository = transitionRepository;
        this.paymentGateway = paymentGateway;
        this.emailService = emailService;
        this.adminEmail = adminEmail;
    }

    @Transactional
    public Payment createPaymentIntent(Booking booking, Payment.PaymentMethod method) {
        if (booking == null) {
            throw new BusinessRuleException("INVALID_BOOKING", "Booking is required");
        }
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new BusinessRuleException("INVALID_STATE", "Booking is not in a valid state for payment.");
        }

        BigDecimal amount = booking.getTotalPrice();
        String intentId = paymentGateway.createIntent(booking, amount, "VND");

        Payment payment = new Payment(booking, amount, method, Payment.PaymentStatus.PENDING, generateRef());
        payment.setCurrency("VND");
        payment.setGateway(Payment.PaymentGateway.MOCK);
        payment.setIntentId(intentId);
        payment.setCreatedAt(LocalDateTime.now());
        
        Payment saved = paymentRepository.save(payment);
        log.info("Payment intent {} created for booking {} amount={}",
                intentId, booking.getId(), amount);
        return saved;
    }

    @Transactional
    public Payment handleWebhook(String intentId, String status, String rawResponse) {
        Payment payment = paymentRepository.findByIntentId(intentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", intentId));

        payment.setRawResponse(rawResponse);

        // Idempotency: gateways routinely retry webhooks. If we have already
        // moved this payment to a terminal state, do not record the duplicate.
        if (payment.getStatus() == Payment.PaymentStatus.PAID
                || payment.getStatus() == Payment.PaymentStatus.REFUNDED
                || payment.getStatus() == Payment.PaymentStatus.PARTIALLY_REFUNDED
                || payment.getStatus() == Payment.PaymentStatus.FAILED) {
            log.info("Webhook for payment #{} ignored — already in terminal state {}",
                    payment.getId(), payment.getStatus());
            return payment;
        }

        if ("succeeded".equalsIgnoreCase(status)) {
            payment.setStatus(Payment.PaymentStatus.PAID);
            payment.setCompletedAt(LocalDateTime.now());
            recordPaymentTransition(payment, "Webhook reported success for intent "
                    + intentId);
            // Delegate to BookingService.confirmPayment so the booking gets the
            // proper BookingStatusTransition row + email notification. We
            // pass null for amount because the booking already carries the
            // authoritative total price.
            Booking booking = payment.getBooking();
            if (booking != null && booking.getStatus() == BookingStatus.PENDING_PAYMENT) {
                booking.setStatus(BookingStatus.PAID);
                booking.setPaidAt(LocalDateTime.now());
                bookingRepository.save(booking);
                transitionRepository.save(new BookingStatusTransition(
                        booking,
                        BookingStatus.PENDING_PAYMENT,
                        BookingStatus.PAID,
                        null,
                        "Webhook payment confirmation: " + intentId));
                emailService.sendPaymentConfirmedToCustomer(booking);
                emailService.sendPaymentReceivedNotificationToAdmin(booking, adminEmail);
            }
            log.info("Payment {} completed successfully via webhook.", payment.getId());
        } else if ("failed".equalsIgnoreCase(status)) {
            payment.setStatus(Payment.PaymentStatus.FAILED);
            recordPaymentTransition(payment, "Webhook reported failure for intent "
                    + intentId);
            log.warn("Payment {} failed via webhook.", payment.getId());
        }

        return paymentRepository.save(payment);
    }

    private void recordPaymentTransition(Payment payment, String reason) {
        // We don't have a dedicated PaymentStatusTransition entity, so we
        // persist a synthetic Booking transition row to keep the audit trail
        // visible from the booking timeline. The actor is null because the
        // trigger is the gateway, not a human user.
        if (payment.getBooking() != null) {
            transitionRepository.save(new BookingStatusTransition(
                    payment.getBooking(),
                    payment.getBooking().getStatus(),
                    payment.getBooking().getStatus(),
                    null,
                    "[Payment] " + reason));
        }
    }

    @Transactional
    public Payment createPayment(Booking booking, BigDecimal amount, Payment.PaymentMethod method,
                                 String transactionRef, String notes) {
        if (booking == null) {
            throw new BusinessRuleException("INVALID_BOOKING", "Booking is required");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessRuleException("INVALID_AMOUNT", "Số tiền thanh toán phải lớn hơn 0");
        }
        Payment payment = new Payment(booking, amount, method, Payment.PaymentStatus.PENDING,
                transactionRef != null ? transactionRef : generateRef());
        payment.setNotes(notes);
        payment.setCreatedAt(LocalDateTime.now());
        Payment saved = paymentRepository.save(payment);
        log.info("Payment record {} created for booking {} amount={}",
                saved.getId(), booking.getId(), amount);
        return saved;
    }

    @Transactional
    public Payment markCompleted(Integer paymentId, User processedBy) {
        Payment payment = loadOrThrow(paymentId);
        if (payment.getStatus() == Payment.PaymentStatus.PAID) {
            return payment;
        }
        if (payment.getStatus() != Payment.PaymentStatus.PENDING) {
            throw new BusinessRuleException("INVALID_STATE",
                    "Không thể hoàn thành thanh toán ở trạng thái " + payment.getStatus());
        }
        payment.setStatus(Payment.PaymentStatus.PAID);
        payment.setCompletedAt(LocalDateTime.now());
        payment.setProcessedBy(processedBy);
        Payment saved = paymentRepository.save(payment);

        // Move the booking to CONFIRMED.
        Booking booking = payment.getBooking();
        if (booking.getStatus() == BookingStatus.PENDING_PAYMENT) {
            booking.setStatus(BookingStatus.PAID);
            booking.setPaidAt(LocalDateTime.now());
            bookingRepository.save(booking);
        }
        log.info("Payment {} marked PAID by {}", paymentId,
                processedBy != null ? processedBy.getUsername() : "system");
        return saved;
    }

    @Transactional
    public Payment markFailed(Integer paymentId, String reason) {
        Payment payment = loadOrThrow(paymentId);
        if (payment.getStatus() == Payment.PaymentStatus.FAILED
                || payment.getStatus() == Payment.PaymentStatus.REFUNDED) {
            return payment;
        }
        payment.setStatus(Payment.PaymentStatus.FAILED);
        if (reason != null) {
            payment.setNotes((payment.getNotes() != null ? payment.getNotes() + "\n" : "")
                    + "Failure: " + reason);
        }
        Payment saved = paymentRepository.save(payment);
        log.warn("Payment {} marked FAILED: {}", paymentId, reason);
        return saved;
    }

    @Transactional
    public Payment refund(Integer paymentId, BigDecimal refundAmount, String reason, User processedBy) {
        Payment payment = loadOrThrow(paymentId);
        // Idempotency: a fully-refunded payment cannot be refunded again.
        if (payment.getStatus() == Payment.PaymentStatus.REFUNDED) {
            log.info("Refund requested for payment #{} which is already fully refunded", paymentId);
            return payment;
        }
        if (payment.getStatus() != Payment.PaymentStatus.PAID
                && payment.getStatus() != Payment.PaymentStatus.PARTIALLY_REFUNDED) {
            throw new BusinessRuleException("INVALID_STATE",
                    "Chỉ hoàn tiền cho thanh toán đã hoàn thành");
        }
        if (refundAmount == null || refundAmount.signum() <= 0) {
            throw new BusinessRuleException("INVALID_AMOUNT", "Số tiền hoàn phải lớn hơn 0");
        }
        BigDecimal alreadyRefunded = payment.getRefundAmount() != null
                ? payment.getRefundAmount() : BigDecimal.ZERO;
        BigDecimal totalRefund = alreadyRefunded.add(refundAmount);
        if (totalRefund.compareTo(payment.getAmount()) > 0) {
            throw new BusinessRuleException("REFUND_OVERFLOW",
                    "Tổng tiền hoàn vượt quá số tiền đã thanh toán");
        }
        payment.setRefundAmount(totalRefund);
        payment.setRefundReason(reason);
        payment.setRefundedAt(LocalDateTime.now());
        payment.setProcessedBy(processedBy);
        payment.setStatus(totalRefund.compareTo(payment.getAmount()) >= 0
                ? Payment.PaymentStatus.REFUNDED
                : Payment.PaymentStatus.PARTIALLY_REFUNDED);

        // Call gateway to refund
        paymentGateway.refund(payment, refundAmount);

        Payment saved = paymentRepository.save(payment);

        // Sync the booking side: record the refund so the booking summary
        // shows what was returned and persists an audit trail row.
        Booking booking = payment.getBooking();
        if (booking != null) {
            BigDecimal bookingRefund = booking.getRefundAmount() != null
                    ? booking.getRefundAmount() : BigDecimal.ZERO;
            booking.setRefundAmount(bookingRefund.add(refundAmount));
            bookingRepository.save(booking);
            transitionRepository.save(new BookingStatusTransition(
                    booking,
                    booking.getStatus(),
                    booking.getStatus(),
                    processedBy,
                    "[Payment] Refund " + refundAmount + " (" + reason + ")"));
        }

        log.info("Payment {} refunded amount={} by {}", paymentId, refundAmount,
                processedBy != null ? processedBy.getUsername() : "system");
        return saved;
    }

    public List<Payment> getPaymentsByBooking(Integer bookingId) {
        return paymentRepository.findByBookingIdOrderByCreatedAtDesc(bookingId);
    }

    public List<Payment> getPaymentsByUser(Integer userId) {
        return paymentRepository.findByUserId(userId);
    }

    public List<Payment> getPaymentsByStatus(Payment.PaymentStatus status) {
        return paymentRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    private Payment loadOrThrow(Integer id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", id));
    }

    private String generateRef() {
        return "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}