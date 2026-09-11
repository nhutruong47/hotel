package com.hsf.hotel.payment.repository;

import com.hsf.hotel.payment.model.PaymentRefund;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRefundRepository extends JpaRepository<PaymentRefund, Long> {
    Optional<PaymentRefund> findByPaymentIdAndIdempotencyKey(Integer paymentId, String idempotencyKey);
}
