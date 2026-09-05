package com.hsf.hotel.config;
import com.hsf.hotel.payment.model.Payment;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;

/**
 * Stripe API configuration.
 * 
 * <p>Initializes Stripe API with the secret key from configuration.
 * Supports both test and live modes based on the key prefix.
 */
@Configuration
public class StripeConfig {

    private static final Logger log = LoggerFactory.getLogger(StripeConfig.class);

    @Value("${stripe.secret-key:}")
    @Nullable
    private String secretKey;

    @Value("${stripe.api-version:2024-12-18.acacia}")
    private String apiVersion;

    @PostConstruct
    public void init() {
        if (secretKey == null || secretKey.isBlank()) {
            log.warn("⚠️  Stripe secret key not configured. Payment gateway will use mock mode.");
            log.warn("⚠️  Set STRIPE_SECRET_KEY environment variable for real payments.");
            return;
        }

        // Mask the key for logging
        String maskedKey = maskKey(secretKey);
        boolean isTestMode = secretKey.startsWith("sk_test_");
        
        log.info("Stripe configured: mode={}, version={}", 
                isTestMode ? "TEST" : "LIVE", apiVersion);
        log.debug("Stripe key: {}", maskedKey);

        Stripe.apiKey = secretKey;
    }

    public boolean isConfigured() {
        return secretKey != null && !secretKey.isBlank();
    }

    public boolean isTestMode() {
        return secretKey != null && secretKey.startsWith("sk_test_");
    }

    private String maskKey(String key) {
        if (key == null || key.length() < 10) {
            return "***";
        }
        return key.substring(0, 7) + "..." + key.substring(key.length() - 4);
    }
}
