package com.hsf.hotel.service.payment;

import com.hsf.hotel.model.Booking;
import com.hsf.hotel.model.Payment;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
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
