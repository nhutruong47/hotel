package com.hsf.hotel.service.payment;

import com.hsf.hotel.model.Booking;
import com.hsf.hotel.model.Payment;
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
}
