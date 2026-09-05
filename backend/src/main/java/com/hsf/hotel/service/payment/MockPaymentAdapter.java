package com.hsf.hotel.service.payment;

import com.hsf.hotel.booking.model.Booking;
import com.hsf.hotel.payment.model.Payment;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "stripe.enabled", havingValue = "false", matchIfMissing = true)
public class MockPaymentAdapter implements PaymentGateway {

    @Override
    public String createIntent(Booking booking, BigDecimal amount, String currency) {
        // Return a mock intent id
        return "pi_mock_" + UUID.randomUUID().toString().replace("-", "");
    }

    @Override
    public boolean capture(Payment payment) {
        // Mock capture always succeeds
        payment.setRawResponse("{\"status\": \"succeeded\"}");
        return true;
    }

    @Override
    public boolean refund(Payment payment, BigDecimal amount) {
        // Mock refund always succeeds
        payment.setRawResponse("{\"status\": \"refunded\", \"amount\": " + amount + "}");
        return true;
    }
}
