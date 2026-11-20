package com.hsf.hotel.payment.repository;

import com.hsf.hotel.payment.model.PaymentRefund;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PaymentRefundRepository extends JpaRepository<PaymentRefund, Long> {
    Optional<PaymentRefund> findByPaymentIdAndIdempotencyKey(Integer paymentId, String idempotencyKey);

    /** Refunds of a payment in the given states (used to reserve amounts not yet paid out). */
    List<PaymentRefund> findByPaymentIdAndStatusIn(Integer paymentId, Collection<PaymentRefund.RefundStatus> statuses);

    List<PaymentRefund> findByPaymentIdOrderByCreatedAtDesc(Integer paymentId);

    long countByStatus(PaymentRefund.RefundStatus status);

    /** ADMIN work queue: manual refunds waiting for a bank transfer (ADR-0009 Q5). */
    @EntityGraph(attributePaths = {"payment", "payment.booking", "processedBy"})
    List<PaymentRefund> findByStatusOrderByCreatedAtAsc(PaymentRefund.RefundStatus status);
}
