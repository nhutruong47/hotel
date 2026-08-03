package com.hsf.hotel.service.payment;

import com.hsf.hotel.config.StripeConfig;
import com.hsf.hotel.model.Booking;
import com.hsf.hotel.model.Payment;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentConfirmParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import org.springframework.context.annotation.Primary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Stripe payment gateway adapter.
 * 
 * <p>Implements real Stripe payments with:
 * <ul>
 *   <li>Payment Intent creation</li>
 *   <li>Payment confirmation</li>
 *   <li>Refund processing</li>
 *   <li>Webhook signature verification</li>
 * </ul>
 * 
 * <p> Falls back to mock behavior when Stripe is not configured.
 */
@Component
@Primary
public class StripePaymentAdapter implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(StripePaymentAdapter.class);

    private final StripeConfig stripeConfig;
    private final boolean enabled;
    private final String webhookSecret;

    public StripePaymentAdapter(
            StripeConfig stripeConfig,
            @Value("${stripe.enabled:true}") boolean enabled,
            @Value("${stripe.webhook-secret:}") String webhookSecret) {
        this.stripeConfig = stripeConfig;
        this.enabled = enabled && stripeConfig.isConfigured();
        this.webhookSecret = webhookSecret;
        
        if (!enabled) {
            log.info("Stripe payment adapter is disabled via configuration.");
        } else if (!stripeConfig.isConfigured()) {
            log.warn("Stripe is enabled but not configured - falling back to mock mode.");
        } else {
            log.info("Stripe payment adapter initialized in {} mode.", 
                    stripeConfig.isTestMode() ? "TEST" : "LIVE");
        }
    }

    @Override
    public String createIntent(Booking booking, BigDecimal amount, String currency) {
        return createIntentDetails(booking, amount, currency).intentId();
    }

    public IntentDetails createIntentDetails(Booking booking, BigDecimal amount, String currency) {
        if (!enabled || !stripeConfig.isConfigured()) {
            log.debug("Stripe not configured, returning mock intent");
            return new IntentDetails(createMockIntent(booking, amount, currency), null);
        }

        try {
            // Convert amount to smallest currency unit (VND has 0 decimals)
            long amountInSmallestUnit = convertToSmallestUnit(amount, currency);

            // Build metadata for tracking
            Map<String, String> metadata = new HashMap<>();
            metadata.put("booking_id", String.valueOf(booking.getId()));
            metadata.put("booking_reference", "NV-" + booking.getId());
            if (booking.getUser() != null) {
                metadata.put("user_id", String.valueOf(booking.getUser().getId()));
                metadata.put("user_email", booking.getGuestEmail() != null 
                        ? booking.getGuestEmail() 
                        : booking.getUser().getEmail());
            }

            PaymentIntentCreateParams.Builder paramsBuilder = PaymentIntentCreateParams.builder()
                    .setAmount(amountInSmallestUnit)
                    .setCurrency(currency.toLowerCase())
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build()
                    )
                    .putAllMetadata(metadata);

            // Add description
            String description = String.format("Booking #%d - %s to %s", 
                    booking.getId(),
                    booking.getCheckInDate(),
                    booking.getCheckOutDate());
            paramsBuilder.setDescription(description);

            PaymentIntent intent = PaymentIntent.create(paramsBuilder.build());

            log.info("Created Stripe PaymentIntent: {} for booking #{} amount={} {}", 
                    intent.getId(), booking.getId(), amount, currency);

            return new IntentDetails(intent.getId(), intent.getClientSecret());

        } catch (StripeException e) {
            log.error("Failed to create Stripe PaymentIntent for booking #{}: {}", 
                    booking.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to create payment intent: " + e.getMessage(), e);
        }
    }

    public record IntentDetails(String intentId, String clientSecret) {}

    @Override
    public boolean capture(Payment payment) {
        if (!enabled || !stripeConfig.isConfigured()) {
            log.debug("Stripe not configured, mock capture success");
            payment.setRawResponse("{\"status\": \"succeeded\", \"mode\": \"mock\"}");
            return true;
        }

        if (payment.getIntentId() == null || payment.getIntentId().startsWith("pi_mock_")) {
            log.debug("Mock intent detected, simulating capture");
            payment.setRawResponse("{\"status\": \"succeeded\", \"mode\": \"mock\"}");
            return true;
        }

        try {
            PaymentIntent intent = PaymentIntent.retrieve(payment.getIntentId());
            
            if ("succeeded".equals(intent.getStatus())) {
                payment.setRawResponse(String.format(
                        "{\"status\": \"%s\", \"amount\": %d}", 
                        intent.getStatus(), intent.getAmount()));
                log.info("Payment {} captured successfully via Stripe", payment.getIntentId());
                return true;
            }

            // Try to confirm if not already
            if ("requires_payment_method".equals(intent.getStatus()) 
                    || "requires_confirmation".equals(intent.getStatus())) {
                PaymentIntentConfirmParams params = PaymentIntentConfirmParams.builder().build();
                intent = intent.confirm(params);
                
                if ("succeeded".equals(intent.getStatus())) {
                    payment.setRawResponse(String.format(
                            "{\"status\": \"%s\", \"amount\": %d}", 
                            intent.getStatus(), intent.getAmount()));
                    return true;
                }
            }

            payment.setRawResponse(String.format(
                    "{\"status\": \"%s\", \"last_payment_error\": \"%s\"}", 
                    intent.getStatus(), 
                    intent.getLastPaymentError() != null 
                            ? intent.getLastPaymentError().getMessage() 
                            : "unknown"));
            return false;

        } catch (StripeException e) {
            log.error("Failed to capture payment {}: {}", payment.getIntentId(), e.getMessage());
            payment.setRawResponse("{\"error\": \"" + e.getMessage() + "\"}");
            return false;
        }
    }

    @Override
    public boolean refund(Payment payment, BigDecimal amount) {
        if (!enabled || !stripeConfig.isConfigured()) {
            log.debug("Stripe not configured, mock refund success");
            payment.setRawResponse(String.format(
                    "{\"status\": \"refunded\", \"amount\": %s, \"mode\": \"mock\"}", amount.toPlainString()));
            return true;
        }

        if (payment.getIntentId() == null || payment.getIntentId().startsWith("pi_mock_")) {
            log.debug("Mock intent detected, simulating refund");
            payment.setRawResponse(String.format(
                    "{\"status\": \"refunded\", \"amount\": %s, \"mode\": \"mock\"}", amount.toPlainString()));
            return true;
        }

        try {
            // Get the original payment to find the charge ID
            PaymentIntent intent = PaymentIntent.retrieve(payment.getIntentId());
            
            if (intent.getLatestCharge() == null) {
                log.error("No charge found for PaymentIntent {}", payment.getIntentId());
                payment.setRawResponse("{\"error\": \"No charge found\"}");
                return false;
            }

            long refundAmount = convertToSmallestUnit(amount, payment.getCurrency());

            RefundCreateParams params = RefundCreateParams.builder()
                    .setCharge(intent.getLatestCharge())
                    .setAmount(refundAmount)
                    .putMetadata("booking_id", String.valueOf(payment.getBooking().getId()))
                    .build();

            com.stripe.model.Refund refund = com.stripe.model.Refund.create(params);

            payment.setRawResponse(String.format(
                    "{\"status\": \"%s\", \"amount\": %d, \"refund_id\": \"%s\"}",
                    refund.getStatus(),
                    refund.getAmount(),
                    refund.getId()));

            log.info("Refund {} created for payment {} amount={}", 
                    refund.getId(), payment.getIntentId(), amount);
            return "succeeded".equals(refund.getStatus()) || "pending".equals(refund.getStatus());

        } catch (StripeException e) {
            log.error("Failed to refund payment {}: {}", payment.getIntentId(), e.getMessage());
            payment.setRawResponse("{\"error\": \"" + e.getMessage() + "\"}");
            return false;
        }
    }

    /**
     * Verifies Stripe webhook signature.
     * 
     * @param payload The raw request body
     * @param signatureHeader The Stripe-Signature header value
     * @return The parsed Event object
     * @throws RuntimeException if signature verification fails
     */
    public Event verifyWebhookSignature(String payload, String signatureHeader) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new RuntimeException("Stripe webhook secret is not configured");
        }

        try {
            return Webhook.constructEvent(payload, signatureHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.error("Webhook signature verification failed: {}", e.getMessage());
            throw new RuntimeException("Invalid webhook signature", e);
        }
    }

    /**
     * Retrieves the status of a PaymentIntent.
     */
    public String getPaymentIntentStatus(String intentId) {
        if (!enabled || !stripeConfig.isConfigured() || intentId.startsWith("pi_mock_")) {
            return "succeeded"; // Mock always succeeds
        }

        try {
            PaymentIntent intent = PaymentIntent.retrieve(intentId);
            return intent.getStatus();
        } catch (StripeException e) {
            log.error("Failed to retrieve PaymentIntent {}: {}", intentId, e.getMessage());
            return "unknown";
        }
    }

    /**
     * Converts amount to smallest currency unit.
     * VND uses 0 decimals, USD uses 2 decimals.
     */
    private long convertToSmallestUnit(BigDecimal amount, String currency) {
        if ("VND".equalsIgnoreCase(currency)) {
            return amount.longValue(); // VND has no decimals
        }
        // Default: assume 2 decimal places
        return amount.multiply(BigDecimal.valueOf(100)).longValue();
    }

    /**
     * Creates a mock intent when Stripe is not configured.
     */
    private String createMockIntent(Booking booking, BigDecimal amount, String currency) {
        String mockId = "pi_mock_" + java.util.UUID.randomUUID().toString().replace("-", "");
        log.debug("Created mock PaymentIntent: {} for booking #{}", mockId, booking.getId());
        return mockId;
    }

    /**
     * Check if real Stripe is enabled and configured.
     */
    public boolean isRealStripeEnabled() {
        return enabled && stripeConfig.isConfigured();
    }
}
