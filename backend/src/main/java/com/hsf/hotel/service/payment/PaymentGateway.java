package com.hsf.hotel.service.payment;

import com.hsf.hotel.booking.model.Booking;
import com.hsf.hotel.payment.model.Payment;
import java.math.BigDecimal;

public interface PaymentGateway {

    /**
     * Initializes a payment intent or transaction with the provider.
     * Returns the intent ID or transaction reference string.
     */
    String createIntent(Booking booking, BigDecimal amount, String currency);

    /**
     * Captures an authorized payment.
     * @return true if successfully captured, false otherwise.
     */
    boolean capture(Payment payment);

    /**
     * Refunds a payment.
     * @return true if successfully refunded, false otherwise.
     */
    boolean refund(Payment payment, BigDecimal amount);

    /**
     * Refunds with a provider-level idempotency key. Gateways that do not
     * support it may fall back to the legacy operation.
     */
    default boolean refund(Payment payment, BigDecimal amount, String idempotencyKey) {
        return refund(payment, amount);
    }
}
