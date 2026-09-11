package com.hsf.hotel.payment.service;
import com.hsf.hotel.notification.service.NotificationProducer;

import com.hsf.hotel.notification.dto.NotificationEvent;
import com.hsf.hotel.exception.BusinessRuleException;
import com.hsf.hotel.exception.ExternalServiceException;
import com.hsf.hotel.exception.ResourceNotFoundException;
import com.hsf.hotel.booking.model.Booking;
import com.hsf.hotel.booking.model.BookingStatus;
import com.hsf.hotel.booking.model.BookingStateMachine;
import com.hsf.hotel.payment.model.Payment;
import com.hsf.hotel.payment.model.PaymentRefund;
import com.hsf.hotel.user.model.User;
import com.hsf.hotel.booking.repository.BookingRepository;
import com.hsf.hotel.payment.repository.PaymentRepository;
import com.hsf.hotel.payment.repository.PaymentRefundRepository;
import com.hsf.hotel.booking.model.BookingStatusTransition;
import com.hsf.hotel.booking.repository.BookingStatusTransitionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.hsf.hotel.service.payment.PaymentGateway;
import com.hsf.hotel.service.payment.StripePaymentAdapter;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final PaymentRefundRepository paymentRefundRepository;
    private final BookingRepository bookingRepository;
    private final BookingStatusTransitionRepository transitionRepository;
    private final PaymentGateway paymentGateway;
    private final NotificationProducer notificationProducer;
    @Value("${app.admin.email:admin@hotel.com}")
    private String adminEmail = "admin@hotel.com";

    public PaymentService(PaymentRepository paymentRepository,
                          PaymentRefundRepository paymentRefundRepository,
                          BookingRepository bookingRepository,
                          BookingStatusTransitionRepository transitionRepository,
                          PaymentGateway paymentGateway,
                          NotificationProducer notificationProducer) {
        this.paymentRepository = paymentRepository;
        this.paymentRefundRepository = paymentRefundRepository;
        this.bookingRepository = bookingRepository;
        this.transitionRepository = transitionRepository;
        this.paymentGateway = paymentGateway;
        this.notificationProducer = notificationProducer;
    }

    @Transactional
    public Payment createPaymentIntent(Booking booking, Payment.PaymentMethod method) {
        return createPaymentIntentWithClientSecret(booking, method).payment();
    }

    public record PaymentIntentResult(Payment payment, String clientSecret) {}

    @Transactional
    public PaymentIntentResult createPaymentIntentWithClientSecret(Booking booking, Payment.PaymentMethod method) {
        if (booking == null) {
            throw new BusinessRuleException("INVALID_BOOKING", "Booking is required");
        }
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new BusinessRuleException("INVALID_STATE", "Booking is not in a valid state for payment.");
        }

        BigDecimal amount = booking.getTotalPrice();
        String intentId;
        String clientSecret = null;
        if (paymentGateway instanceof StripePaymentAdapter stripeGateway) {
            StripePaymentAdapter.IntentDetails details = stripeGateway.createIntentDetails(booking, amount, "VND");
            intentId = details.intentId();
            clientSecret = details.clientSecret();
        } else {
            intentId = paymentGateway.createIntent(booking, amount, "VND");
        }

        Payment payment = new Payment(booking, amount, method, Payment.PaymentStatus.PENDING, generateRef());
        payment.setCurrency("VND");
        payment.setGateway(clientSecret != null ? Payment.PaymentGateway.STRIPE : Payment.PaymentGateway.MOCK);
        payment.setIntentId(intentId);
        payment.setCreatedAt(LocalDateTime.now());
        
        Payment saved = paymentRepository.save(payment);
        log.info("Payment intent {} created for booking {} amount={}",
                intentId, booking.getId(), amount);
        return new PaymentIntentResult(saved, clientSecret);
    }

    @Transactional
    public Payment handleWebhook(String intentId, String status, String rawResponse) {
        Payment payment = paymentRepository.findByIntentIdForUpdate(intentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", intentId));
        return processWebhook(payment, status, rawResponse, intentId);
    }

    @Transactional(readOnly = true)
    public Payment getPaymentByTransactionRef(String transactionRef) {
        if (transactionRef == null || transactionRef.isBlank()) {
            throw new ResourceNotFoundException("Payment", transactionRef);
        }
        return paymentRepository.findByTransactionRef(transactionRef)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", transactionRef));
    }

    @Transactional
    public Payment handleCheckoutWebhook(String sessionId, String discoveredIntentId, String rawResponse) {
        Payment payment = paymentRepository.findByTransactionRefForUpdate(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", sessionId));
        if (discoveredIntentId != null && !discoveredIntentId.isBlank()) {
            if (payment.getIntentId() != null && !payment.getIntentId().equals(discoveredIntentId)) {
                throw new BusinessRuleException("PAYMENT_CORRELATION_MISMATCH",
                        "Checkout session is linked to a different payment intent");
            }
            payment.setIntentId(discoveredIntentId);
        }
        return processWebhook(payment, "succeeded", rawResponse, sessionId);
    }

    private Payment processWebhook(Payment payment, String status, String rawResponse, String correlationId) {

        payment.setRawResponse(rawResponse);

        // Idempotency: gateways routinely retry webhooks. If we have already
        // moved this payment to a terminal state, do not record the duplicate.
        boolean succeeded = "succeeded".equalsIgnoreCase(status);
        boolean failed = "failed".equalsIgnoreCase(status);
        if (!succeeded && !failed) {
            throw new BusinessRuleException("INVALID_PAYMENT_STATUS",
                    "Unsupported gateway payment status");
        }
        if (payment.getStatus() == Payment.PaymentStatus.PAID
                || payment.getStatus() == Payment.PaymentStatus.REFUNDED
                || payment.getStatus() == Payment.PaymentStatus.PARTIALLY_REFUNDED
                || (payment.getStatus() == Payment.PaymentStatus.FAILED && failed)) {
            log.info("Webhook for payment #{} ignored — already in terminal state {}",
                    payment.getId(), payment.getStatus());
            return payment;
        }

        if (succeeded) {
            payment.setStatus(Payment.PaymentStatus.PAID);
            payment.setCompletedAt(LocalDateTime.now());
            Booking bookingRef = payment.getBooking();
            Booking booking = bookingRef != null
                    ? bookingRepository.findByIdForUpdate(bookingRef.getId()).orElse(bookingRef)
                    : null;
            if (booking != null && booking.getStatus() == BookingStatus.PENDING_PAYMENT) {
                if (booking.getTotalPrice() == null || booking.getTotalPrice().compareTo(payment.getAmount()) != 0) {
                    throw new BusinessRuleException("PAYMENT_AMOUNT_MISMATCH",
                            "Gateway payment amount does not match the booking total");
                }
                BookingStateMachine.requireTransition(booking.getStatus(), BookingStatus.PAID);
                booking.setStatus(BookingStatus.PAID);
                booking.setPaidAt(LocalDateTime.now());
                bookingRepository.save(booking);
                transitionRepository.save(new BookingStatusTransition(
                        booking,
                        BookingStatus.PENDING_PAYMENT,
                        BookingStatus.PAID,
                        null,
                        "Webhook payment confirmation: " + correlationId));
                
                NotificationEvent customerEvent = new NotificationEvent("PAYMENT_CUSTOMER", booking.getId());
                notificationProducer.sendEmailNotification(customerEvent);

                NotificationEvent adminEvent = new NotificationEvent("PAYMENT_ADMIN", booking.getId());
                adminEvent.setAdminEmail(adminEmail);
                notificationProducer.sendEmailNotification(adminEvent);
            } else if (booking != null) {
                payment.setNotes(appendNote(payment.getNotes(),
                        "Late payment received while booking was " + booking.getStatus()
                                + "; manual review/refund required"));
                recordPaymentTransition(payment,
                        "Late payment received for booking in " + booking.getStatus());
            }
            log.info("Payment {} completed successfully via webhook.", payment.getId());
        } else if (failed) {
            payment.setStatus(Payment.PaymentStatus.FAILED);
            recordPaymentTransition(payment, "Webhook reported failure for intent "
                    + correlationId);
            log.warn("Payment {} failed via webhook.", payment.getId());
        }

        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment createCheckoutPayment(Booking booking, String sessionId, String intentId) {
        if (booking == null || booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new BusinessRuleException("INVALID_STATE",
                    "Booking is not in a valid state for checkout payment");
        }
        if (sessionId == null || sessionId.isBlank()) {
            throw new BusinessRuleException("INVALID_PAYMENT_SESSION", "Checkout session ID is required");
        }
        Payment existing = paymentRepository.findByTransactionRef(sessionId).orElse(null);
        if (existing != null) {
            if (existing.getBooking().getId().equals(booking.getId())) {
                return existing;
            }
            throw new BusinessRuleException("PAYMENT_CORRELATION_MISMATCH",
                    "Checkout session is already linked to another booking");
        }
        Payment payment = new Payment(booking, booking.getTotalPrice(), Payment.PaymentMethod.CARD,
                Payment.PaymentStatus.PENDING, sessionId);
        payment.setGateway(Payment.PaymentGateway.STRIPE);
        payment.setCurrency("VND");
        payment.setIntentId(intentId);
        payment.setCreatedAt(LocalDateTime.now());
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
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new BusinessRuleException("INVALID_STATE",
                    "Booking is not in a valid state for payment");
        }
        if (booking.getTotalPrice() == null || booking.getTotalPrice().compareTo(amount) != 0) {
            throw new BusinessRuleException("PAYMENT_AMOUNT_MISMATCH",
                    "Payment amount must equal the booking total");
        }
        if (method == null) {
            throw new BusinessRuleException("INVALID_PAYMENT_METHOD", "Payment method is required");
        }
        if (transactionRef != null && !transactionRef.isBlank()) {
            Payment existing = paymentRepository.findByTransactionRef(transactionRef.trim()).orElse(null);
            if (existing != null) {
                boolean sameRequest = existing.getBooking().getId().equals(booking.getId())
                        && existing.getAmount().compareTo(amount) == 0
                        && existing.getMethod() == method;
                if (sameRequest) {
                    return existing;
                }
                throw new BusinessRuleException("DUPLICATE_TRANSACTION_REFERENCE",
                        "Transaction reference is already used by another payment");
            }
        }
        Payment payment = new Payment(booking, amount, method, Payment.PaymentStatus.PENDING,
                transactionRef != null && !transactionRef.isBlank() ? transactionRef.trim() : generateRef());
        payment.setNotes(notes);
        payment.setCreatedAt(LocalDateTime.now());
        Payment saved = paymentRepository.save(payment);
        log.info("Payment record {} created for booking {} amount={}",
                saved.getId(), booking.getId(), amount);
        return saved;
    }

    @Transactional
    public Payment markCompleted(Integer paymentId, User processedBy) {
        Payment payment = loadForUpdateOrThrow(paymentId);
        if (payment.getStatus() == Payment.PaymentStatus.PAID) {
            return payment;
        }
        if (payment.getStatus() != Payment.PaymentStatus.PENDING) {
            throw new BusinessRuleException("INVALID_STATE",
                    "Không thể hoàn thành thanh toán ở trạng thái " + payment.getStatus());
        }
        // Keep the booking and the manually confirmed payment in one transaction.
        Booking bookingRef = payment.getBooking();
        if (bookingRef == null) {
            throw new BusinessRuleException("INVALID_BOOKING_STATE",
                    "Payment is not attached to a booking");
        }
        Booking booking = bookingRepository.findByIdForUpdate(bookingRef.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingRef.getId()));
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new BusinessRuleException("INVALID_BOOKING_STATE",
                    "The booking is no longer awaiting payment");
        }
        if (booking.getTotalPrice() == null || booking.getTotalPrice().compareTo(payment.getAmount()) != 0) {
            throw new BusinessRuleException("PAYMENT_AMOUNT_MISMATCH",
                    "Payment amount does not match the booking total");
        }

        payment.setStatus(Payment.PaymentStatus.PAID);
        payment.setCompletedAt(LocalDateTime.now());
        payment.setProcessedBy(processedBy);
        Payment saved = paymentRepository.save(payment);

        BookingStateMachine.requireTransition(booking.getStatus(), BookingStatus.PAID);
        booking.setStatus(BookingStatus.PAID);
        booking.setPaidAt(LocalDateTime.now());
        bookingRepository.save(booking);
        transitionRepository.save(new BookingStatusTransition(
                booking,
                BookingStatus.PENDING_PAYMENT,
                BookingStatus.PAID,
                processedBy,
                "Manual payment confirmed: " + payment.getTransactionRef()));
        log.info("Payment {} marked PAID by {}", paymentId,
                processedBy != null ? processedBy.getUsername() : "system");
        return saved;
    }

    @Transactional
    public Payment markFailed(Integer paymentId, String reason) {
        Payment payment = loadForUpdateOrThrow(paymentId);
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

    @Transactional(noRollbackFor = ExternalServiceException.class)
    public Payment refund(Integer paymentId, BigDecimal refundAmount, String reason,
                          User processedBy, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 128) {
            throw new BusinessRuleException("INVALID_IDEMPOTENCY_KEY",
                    "A valid Idempotency-Key header is required");
        }
        if (refundAmount == null || refundAmount.signum() <= 0) {
            throw new BusinessRuleException("INVALID_AMOUNT", "Số tiền hoàn phải lớn hơn 0");
        }
        Payment payment = loadForUpdateOrThrow(paymentId);
        PaymentRefund previous = paymentRefundRepository
                .findByPaymentIdAndIdempotencyKey(paymentId, idempotencyKey.trim())
                .orElse(null);
        if (previous != null) {
            if (previous.getAmount().compareTo(refundAmount) != 0) {
                throw new BusinessRuleException("IDEMPOTENCY_KEY_REUSED",
                        "Idempotency key was already used with a different refund amount");
            }
            if (previous.getStatus() == PaymentRefund.RefundStatus.SUCCEEDED) {
                return payment;
            }
            if (previous.getStatus() == PaymentRefund.RefundStatus.FAILED) {
                throw new ExternalServiceException("Previous refund attempt failed");
            }
            throw new BusinessRuleException("REFUND_IN_PROGRESS", "Refund is already being processed");
        }

        // A fully-refunded payment cannot be refunded again under a new key.
        if (payment.getStatus() == Payment.PaymentStatus.REFUNDED) {
            throw new BusinessRuleException("ALREADY_REFUNDED", "Payment is already fully refunded");
        }
        if (payment.getStatus() != Payment.PaymentStatus.PAID
                && payment.getStatus() != Payment.PaymentStatus.PARTIALLY_REFUNDED) {
            throw new BusinessRuleException("INVALID_STATE",
                    "Chỉ hoàn tiền cho thanh toán đã hoàn thành");
        }
        BigDecimal alreadyRefunded = payment.getRefundAmount() != null
                ? payment.getRefundAmount() : BigDecimal.ZERO;
        BigDecimal totalRefund = alreadyRefunded.add(refundAmount);
        if (totalRefund.compareTo(payment.getAmount()) > 0) {
            throw new BusinessRuleException("REFUND_OVERFLOW",
                    "Tổng tiền hoàn vượt quá số tiền đã thanh toán");
        }
        Booking bookingRef = payment.getBooking();
        Booking booking = null;
        if (bookingRef != null) {
            booking = bookingRepository.findByIdForUpdate(bookingRef.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingRef.getId()));
        }

        PaymentRefund refund = new PaymentRefund();
        refund.setPayment(payment);
        refund.setIdempotencyKey(idempotencyKey.trim());
        refund.setAmount(refundAmount);
        refund.setReason(reason);
        refund.setProcessedBy(processedBy);
        paymentRefundRepository.save(refund);

        boolean gatewaySucceeded;
        try {
            gatewaySucceeded = paymentGateway.refund(payment, refundAmount, idempotencyKey.trim());
        } catch (RuntimeException ex) {
            refund.setStatus(PaymentRefund.RefundStatus.FAILED);
            refund.setCompletedAt(LocalDateTime.now());
            paymentRefundRepository.save(refund);
            throw new ExternalServiceException("Payment gateway refund failed", ex);
        }
        if (!gatewaySucceeded) {
            refund.setStatus(PaymentRefund.RefundStatus.FAILED);
            refund.setCompletedAt(LocalDateTime.now());
            paymentRefundRepository.save(refund);
            throw new ExternalServiceException("Payment gateway rejected the refund");
        }

        payment.setRefundAmount(totalRefund);
        payment.setRefundReason(reason);
        payment.setRefundedAt(LocalDateTime.now());
        payment.setProcessedBy(processedBy);
        payment.setStatus(totalRefund.compareTo(payment.getAmount()) >= 0
                ? Payment.PaymentStatus.REFUNDED
                : Payment.PaymentStatus.PARTIALLY_REFUNDED);
        refund.setStatus(PaymentRefund.RefundStatus.SUCCEEDED);
        refund.setCompletedAt(LocalDateTime.now());
        paymentRefundRepository.save(refund);

        Payment saved = paymentRepository.save(payment);

        // Sync the booking side: record the refund so the booking summary
        // shows what was returned and persists an audit trail row.
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

    private Payment loadForUpdateOrThrow(Integer id) {
        return paymentRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", id));
    }

    private static String appendNote(String current, String note) {
        return current == null || current.isBlank() ? note : current + "\n" + note;
    }

    private String generateRef() {
        return "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
