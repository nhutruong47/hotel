package com.hsf.hotel.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Helpers for verifying HMAC signatures on incoming webhook requests from
 * payment gateways. The signing scheme used here is the same convention
 * Stripe publishes: {@code hex(HMAC-SHA256(secret, timestamp + "." + body))},
 * with the value carried in the {@code X-Signature} (or configurable)
 * header and the timestamp in {@code X-Timestamp}.
 *
 * <p>The {@link com.hsf.hotel.config.GatewaySignatureFilter} uses this
 * helper to reject any webhook that does not carry a matching signature,
 * with a 5-minute clock skew window to allow for legitimate replay.
 */
public final class WebhookSignatureVerifier {

    private WebhookSignatureVerifier() {}

    /**
     * Computes the hex-encoded HMAC-SHA256 of {@code timestamp + "." + body}
     * using the supplied secret.
     */
    public static String compute(String secret, long timestamp, String body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            mac.update(Long.toString(timestamp).getBytes(StandardCharsets.UTF_8));
            mac.update((byte) '.');
            mac.update(body.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(mac.doFinal());
        } catch (Exception e) {
            throw new IllegalStateException("Cannot compute HMAC", e);
        }
    }

    /**
     * Constant-time equality check between an expected and a candidate
     * hex-encoded HMAC signature.
     */
    public static boolean matches(String expected, String candidate) {
        if (expected == null || candidate == null) {
            return false;
        }
        byte[] a = expected.toLowerCase().getBytes(StandardCharsets.UTF_8);
        byte[] b = candidate.toLowerCase().getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(a, b);
    }
}