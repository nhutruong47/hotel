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

    /* ---------- ADMIN alerts (setter-injected: optional in unit tests) ---------- */

    private com.hsf.hotel.notification.service.NotificationService notificationService;
    private com.hsf.hotel.user.repository.UserRepository userRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setAdminAlerting(com.hsf.hotel.notification.service.NotificationService notificationService,
                          com.hsf.hotel.user.repository.UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    /** In-app alert to every active ADMIN. Best effort: never breaks the payment transaction. */
    private void notifyAdmins(String title, String message) {
        if (notificationService == null || userRepository == null) {
            log.warn("[ADMIN ALERT] {} — {}", title, message);
            return;
        }
        try {
            for (User admin : userRepository.findByRole(com.hsf.hotel.user.model.UserRole.ADMIN)) {
                if (!Boolean.TRUE.equals(admin.getDisabled())) {
                    notificationService.createNotification(admin, title, message, "PAYMENT", "/admin");
                }
            }
        } catch (RuntimeException ex) {
            log.warn("Failed to notify admins ({}): {}", title, ex.getMessage());
        }
    }

    /**
     * Money received that cannot be applied automatically (ADR-0009 Q4): keep it
     * recorded as PAID, flag it, leave an audit row on the booking timeline and
     * alert ADMIN. Never auto-refund.
     */
    private void flagForReview(Payment payment, String reason) {
        payment.flagForReview(reason);
        recordPaymentTransition(payment, "REVIEW REQUIRED: " + reason);
        notifyAdmins("Thanh toán cần đối soát",
                "Payment #" + payment.getId() + " (booking #"
                        + (payment.getBooking() != null ? payment.getBooking().getId() : "?")
                        + ", " + payment.getAmount() + " VND): " + reason);
        log.warn("Payment {} flagged for review: {}", payment.getId(), reason);
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
        if (paymentGateway instanceof StripePaymentAdapter stripe && stripe.isRealStripeEnabled()) {
            requireChargeableAmount(amount);
        }

        // R-09: a retried intent request must not create a second payable intent.
        PaymentIntentResult reusable = findReusableIntent(booking, amount);
        if (reusable != null) {
            log.info("Reusing payment intent {} for booking {}", reusable.payment().getIntentId(), booking.getId());
            return reusable;
        }

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

    /** Stripe rejects tiny charges; below this the room price is almost certainly not VND (ADR-0009 Q6). */
    public static final BigDecimal MIN_CHARGE_VND = new BigDecimal("10000");

    public static void requireChargeableAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(MIN_CHARGE_VND) < 0) {
            throw new BusinessRuleException("PRICE_CONFIGURATION_INVALID",
                    "Tổng tiền " + amount + " VND thấp hơn mức thanh toán tối thiểu " + MIN_CHARGE_VND
                            + " VND — kiểm tra lại giá villa (hệ thống dùng VND)");
        }
    }

    private PaymentIntentResult findReusableIntent(Booking booking, BigDecimal amount) {
        List<Payment> existing = paymentRepository.findByBookingIdOrderByCreatedAtDesc(booking.getId());
        if (existing == null) {
            return null;
        }
        for (Payment p : existing) {
            boolean candidate = p.getStatus() == Payment.PaymentStatus.PENDING
                    && p.getIntentId() != null
                    && p.getAmount() != null && p.getAmount().compareTo(amount) == 0
                    && (p.getTransactionRef() == null || !p.getTransactionRef().startsWith("cs_"));
            if (!candidate) {
                continue;
            }
            if (p.getGateway() == Payment.PaymentGateway.MOCK) {
                return new PaymentIntentResult(p, null);
            }
            if (p.getGateway() == Payment.PaymentGateway.STRIPE
                    && paymentGateway instanceof StripePaymentAdapter stripe) {
                String secret = stripe.retrieveReusableClientSecret(p.getIntentId());
                if (secret != null) {
                    return new PaymentIntentResult(p, secret);
                }
            }
        }
        return null;
    }

    /** PENDING Stripe Checkout payments of a booking (newest first) — candidates for reuse. */
    @Transactional(readOnly = true)
    public List<Payment> findPendingCheckoutPayments(Integer bookingId) {
        List<Payment> all = paymentRepository.findByBookingIdOrderByCreatedAtDesc(bookingId);
        if (all == null) {
            return List.of();
        }
        return all.stream()
                .filter(p -> p.getStatus() == Payment.PaymentStatus.PENDING)
                .filter(p -> p.getGateway() == Payment.PaymentGateway.STRIPE)
                .filter(p -> p.getTransactionRef() != null && p.getTransactionRef().startsWith("cs_"))
                .toList();
    }

    /**
     * Retire a PENDING checkout payment whose session can no longer be paid (expired,
     * or amount changed after a booking modification). Kept as FAILED for history.
     */
    @Transactional
    public void supersedeCheckoutPayment(Integer paymentId, String reason) {
        Payment payment = loadForUpdateOrThrow(paymentId);
        if (payment.getStatus() != Payment.PaymentStatus.PENDING) {
            return;
        }
        payment.setStatus(Payment.PaymentStatus.FAILED);
        payment.setNotes(appendNote(payment.getNotes(), "Superseded: " + reason));
        paymentRepository.save(payment);
        log.info("Checkout payment {} superseded: {}", paymentId, reason);
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
            boolean amountMatches = booking != null && booking.getTotalPrice() != null
                    && booking.getTotalPrice().compareTo(payment.getAmount()) == 0;
            if (booking != null && booking.getStatus() == BookingStatus.PENDING_PAYMENT && !amountMatches) {
                // Previously this threw, rolled back and made the gateway retry forever
                // while the guest's money went unrecorded (ADR-0009 Q4).
                flagForReview(payment, "Amount mismatch: booking total " + booking.getTotalPrice()
                        + " VND, received " + payment.getAmount() + " VND; booking not confirmed");
            } else if (booking != null && booking.getStatus() == BookingStatus.PENDING_PAYMENT) {
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
                flagForReview(payment, "Late or duplicate payment while booking was " + booking.getStatus());
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

    private static final java.util.Set<PaymentRefund.RefundStatus> OUTSTANDING_REFUND_STATES =
            java.util.EnumSet.of(PaymentRefund.RefundStatus.PROCESSING, PaymentRefund.RefundStatus.PENDING_MANUAL);

    /**
     * Refund a payment (ADMIN). Card payments captured by a real Stripe charge are
     * refunded automatically; every other payment (SePay/bank transfer, manual,
     * mock) creates a {@code PENDING_MANUAL} refund that only becomes effective once
     * ADMIN confirms the bank transfer ({@link #confirmManualRefund}). ADR-0009 Q5.
     */
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
            if (previous.getStatus() == PaymentRefund.RefundStatus.SUCCEEDED
                    || previous.getStatus() == PaymentRefund.RefundStatus.PENDING_MANUAL) {
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
        // Amounts already promised (pending manual transfers, in-flight gateway calls)
        // count against the paid amount, or two requests could over-refund.
        BigDecimal outstanding = BigDecimal.ZERO;
        List<PaymentRefund> open = paymentRefundRepository
                .findByPaymentIdAndStatusIn(paymentId, OUTSTANDING_REFUND_STATES);
        if (open != null) {
            for (PaymentRefund r : open) {
                outstanding = outstanding.add(r.getAmount());
            }
        }
        if (alreadyRefunded.add(outstanding).add(refundAmount).compareTo(payment.getAmount()) > 0) {
            throw new BusinessRuleException("REFUND_OVERFLOW",
                    "Tổng tiền hoàn vượt quá số tiền đã thanh toán");
        }

        PaymentRefund refund = new PaymentRefund();
        refund.setPayment(payment);
        refund.setIdempotencyKey(idempotencyKey.trim());
        refund.setAmount(refundAmount);
        refund.setReason(reason);
        refund.setProcessedBy(processedBy);

        if (!paymentGateway.supportsAutomaticRefund(payment)) {
            refund.setStatus(PaymentRefund.RefundStatus.PENDING_MANUAL);
            paymentRefundRepository.save(refund);
            recordPaymentTransition(payment, "Manual refund requested: " + refundAmount
                    + " VND, awaiting bank transfer confirmation (" + reason + ")");
            notifyAdmins("Yêu cầu hoàn tiền thủ công",
                    "Payment #" + payment.getId() + ": chuyển khoản hoàn " + refundAmount
                            + " VND rồi xác nhận kèm mã giao dịch ngân hàng.");
            log.info("Manual refund of {} requested for payment {} by {}", refundAmount, paymentId,
                    processedBy != null ? processedBy.getUsername() : "system");
            return payment;
        }

        refund.setStatus(PaymentRefund.RefundStatus.PROCESSING);
        paymentRefundRepository.save(refund);

        boolean gatewaySucceeded;
        try {
            gatewaySucceeded = paymentGateway.refund(payment, refundAmount, idempotencyKey.trim());
        } catch (RuntimeException ex) {
            markRefundFailed(refund);
            throw new ExternalServiceException("Payment gateway refund failed", ex);
        }
        if (!gatewaySucceeded) {
            markRefundFailed(refund);
            throw new ExternalServiceException("Payment gateway rejected the refund");
        }
        return applySucceededRefund(payment, refund, processedBy, reason);
    }

    /**
     * ADMIN confirms that a {@code PENDING_MANUAL} refund was transferred, citing the
     * bank transfer reference. Only then do payment/booking refund totals change.
     * Idempotent for the same reference.
     */
    @Transactional
    public Payment confirmManualRefund(Integer paymentId, Long refundId, String bankReference, User admin) {
        if (bankReference == null || bankReference.isBlank() || bankReference.trim().length() > 128) {
            throw new BusinessRuleException("BANK_REFERENCE_REQUIRED",
                    "Cần mã giao dịch ngân hàng (tối đa 128 ký tự) để xác nhận hoàn tiền");
        }
        Payment payment = loadForUpdateOrThrow(paymentId);
        PaymentRefund refund = loadRefundOfPayment(paymentId, refundId);
        String reference = bankReference.trim();
        if (refund.getStatus() == PaymentRefund.RefundStatus.SUCCEEDED) {
            if (reference.equals(refund.getBankReference())) {
                return payment;
            }
            throw new BusinessRuleException("REFUND_ALREADY_CONFIRMED",
                    "Refund was already confirmed with another bank reference");
        }
        if (refund.getStatus() != PaymentRefund.RefundStatus.PENDING_MANUAL) {
            throw new BusinessRuleException("INVALID_STATE",
                    "Chỉ xác nhận được yêu cầu hoàn tiền thủ công đang chờ (hiện tại: " + refund.getStatus() + ")");
        }
        refund.setBankReference(reference);
        refund.setConfirmedBy(admin);
        refund.setConfirmedAt(LocalDateTime.now());
        return applySucceededRefund(payment, refund, admin, refund.getReason());
    }

    /** ADMIN cancels a manual refund request that will not be paid out. */
    @Transactional
    public Payment rejectManualRefund(Integer paymentId, Long refundId, String reason, User admin) {
        Payment payment = loadForUpdateOrThrow(paymentId);
        PaymentRefund refund = loadRefundOfPayment(paymentId, refundId);
        if (refund.getStatus() == PaymentRefund.RefundStatus.FAILED) {
            return payment;
        }
        if (refund.getStatus() != PaymentRefund.RefundStatus.PENDING_MANUAL) {
            throw new BusinessRuleException("INVALID_STATE",
                    "Chỉ hủy được yêu cầu hoàn tiền thủ công đang chờ (hiện tại: " + refund.getStatus() + ")");
        }
        markRefundFailed(refund);
        recordPaymentTransition(payment, "Manual refund #" + refundId + " rejected by "
                + (admin != null ? admin.getUsername() : "system")
                + (reason != null && !reason.isBlank() ? ": " + reason : ""));
        return payment;
    }

    /** Manual refunds waiting for ADMIN to transfer money (oldest first). */
    @Transactional(readOnly = true)
    public List<PaymentRefund> getPendingManualRefunds() {
        return paymentRefundRepository.findByStatusOrderByCreatedAtAsc(PaymentRefund.RefundStatus.PENDING_MANUAL);
    }

    @Transactional(readOnly = true)
    public List<PaymentRefund> getRefundsForPayment(Integer paymentId) {
        return paymentRefundRepository.findByPaymentIdOrderByCreatedAtDesc(paymentId);
    }

    private PaymentRefund loadRefundOfPayment(Integer paymentId, Long refundId) {
        PaymentRefund refund = paymentRefundRepository.findById(refundId)
                .orElseThrow(() -> new ResourceNotFoundException("Refund", refundId));
        if (refund.getPayment() == null || !paymentId.equals(refund.getPayment().getId())) {
            throw new ResourceNotFoundException("Refund", refundId);
        }
        return refund;
    }

    private void markRefundFailed(PaymentRefund refund) {
        refund.setStatus(PaymentRefund.RefundStatus.FAILED);
        refund.setCompletedAt(LocalDateTime.now());
        paymentRefundRepository.save(refund);
    }

    /** Money has actually left our account: update payment + booking totals and history. */
    private Payment applySucceededRefund(Payment payment, PaymentRefund refund, User actor, String reason) {
        BigDecimal refundAmount = refund.getAmount();
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

        payment.setRefundAmount(totalRefund);
        payment.setRefundReason(reason);
        payment.setRefundedAt(LocalDateTime.now());
        payment.setProcessedBy(actor);
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
            // P0-03: booking.refundAmount already holds the policy entitlement set at
            // cancellation (BookingService#calculateRefund). Adding the executed refund
            // on top double-counted it. Once money has actually moved, the booking shows
            // the total refunded across all of its payments (recomputed, so idempotent).
            booking.setRefundAmount(totalRefundedForBooking(booking.getId(), saved));

            boolean isFullRefund = totalRefund.compareTo(payment.getAmount()) >= 0;
            BookingStatus currentBookingStatus = booking.getStatus();
            BookingStatus targetBookingStatus = currentBookingStatus;

            if (isFullRefund && (currentBookingStatus == BookingStatus.PAID || currentBookingStatus == BookingStatus.PENDING_PAYMENT)) {
                targetBookingStatus = BookingStatus.CANCELLED;
                booking.setStatus(BookingStatus.CANCELLED);
                booking.setCancelledAt(LocalDateTime.now());
                booking.setCancelledBy(actor != null ? actor.getUsername() : "system");
                booking.setCancellationReason("Hoàn tiền 100%: " + (reason != null ? reason : "Full refund"));
            }

            bookingRepository.save(booking);
            transitionRepository.save(new BookingStatusTransition(
                    booking,
                    currentBookingStatus,
                    targetBookingStatus,
                    actor,
                    "[Payment] " + (isFullRefund ? "Full Refund " : "Partial Refund ") + refundAmount
                            + " (" + reason + ")"
                            + (refund.getBankReference() != null ? " bank ref " + refund.getBankReference() : "")));
        }

        log.info("Payment {} refunded amount={} by {}", payment.getId(), refundAmount,
                actor != null ? actor.getUsername() : "system");
        return saved;
    }
    /** Sum of refunds executed on every payment of the booking, using the fresh value for {@code current}. */
    private BigDecimal totalRefundedForBooking(Integer bookingId, Payment current) {
        BigDecimal total = current.getRefundAmount() != null ? current.getRefundAmount() : BigDecimal.ZERO;
        List<Payment> payments = paymentRepository.findByBookingIdOrderByCreatedAtDesc(bookingId);
        if (payments != null) {
            for (Payment other : payments) {
                if (other.getId() != null && other.getId().equals(current.getId())) continue;
                if (other.getRefundAmount() != null) total = total.add(other.getRefundAmount());
            }
        }
        return total;
    }

    public java.util.Optional<Payment> getPaymentById(Integer id) {
        return id == null ? java.util.Optional.empty() : paymentRepository.findById(id);
    }

    /**
     * Record a bank transfer that arrived but cannot confirm its booking (wrong
     * amount). Previously the money was only logged; now it is a PAID payment
     * flagged for reconciliation (ADR-0009 Q4). Idempotent per transaction ref.
     */
    @Transactional
    public Payment recordUnappliedTransfer(Booking booking, BigDecimal amount, String transactionRef,
                                           String notes, String reason) {
        Payment existing = paymentRepository.findByTransactionRef(transactionRef).orElse(null);
        if (existing != null) {
            return existing;
        }
        Payment payment = new Payment(booking, amount, Payment.PaymentMethod.BANK_TRANSFER,
                Payment.PaymentStatus.PAID, transactionRef);
        payment.setGateway(Payment.PaymentGateway.SEPAY);
        payment.setCurrency("VND");
        payment.setCreatedAt(LocalDateTime.now());
        payment.setCompletedAt(LocalDateTime.now());
        payment.setNotes(notes);
        Payment saved = paymentRepository.save(payment);
        flagForReview(saved, reason);
        return paymentRepository.save(saved);
    }

    /** Flag an already recorded payment for reconciliation (late/duplicate transfer). */
    @Transactional
    public Payment flagPaymentForReview(Integer paymentId, String reason) {
        Payment payment = loadForUpdateOrThrow(paymentId);
        flagForReview(payment, reason);
        return paymentRepository.save(payment);
    }

    /** Reconciliation queue (ADR-0009 Q4): received money that still needs a human decision. */
    @Transactional(readOnly = true)
    public List<Payment> getPaymentsRequiringReview() {
        return paymentRepository.findByReviewRequiredTrueOrderByCreatedAtDesc();
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
