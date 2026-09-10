package com.hsf.hotel.security;
import com.hsf.hotel.user.model.User;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hsf.hotel.config.ErrorCodes;
import com.hsf.hotel.config.ApiResponse;
import com.hsf.hotel.admin.repository.AuditLogRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Verifies HMAC signatures on incoming payment-gateway webhooks. Requests
 * to {@code /api/v1/payments/webhook/**} must carry:
 * <ul>
 *   <li>{@code X-Webhook-Timestamp} — Unix seconds, must be within 5 minutes of server time.</li>
 *   <li>{@code X-Webhook-Signature} — hex HMAC-SHA256 of {@code timestamp + "." + raw body}
 *       using the gateway-specific secret.</li>
 * </ul>
 *
 * <p>This filter wraps {@link jakarta.servlet.http.HttpServletRequestWrapper}
 * so downstream controllers can still read the request body.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 25)
public class GatewaySignatureFilter extends OncePerRequestFilter {

    private static final String WEBHOOK_PREFIX = "/api/v1/payments/webhook";
    private static final String STRIPE_WEBHOOK_PATH = "/api/v1/payments/webhook/stripe";
    private static final String SEPAY_WEBHOOK_PATH = "/api/v1/payments/webhook/sepay";
    private static final long MAX_SKEW_SECONDS = 300;

    private final ObjectMapper objectMapper;
    private final String defaultSecret;
    private final AuditLogRepository auditLogRepository;
    private final Map<String, Long> replayProtection = new ConcurrentHashMap<>();

    public GatewaySignatureFilter(ObjectMapper objectMapper,
                                  @Value("${app.webhook.secret:}") String defaultSecret,
                                  AuditLogRepository auditLogRepository) {
        this.objectMapper = objectMapper;
        this.defaultSecret = defaultSecret;
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path == null
                || !path.startsWith(WEBHOOK_PREFIX)
                || path.equals(STRIPE_WEBHOOK_PATH)
                || path.equals(SEPAY_WEBHOOK_PATH);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String timestampHeader = request.getHeader("X-Webhook-Timestamp");
        String signature = request.getHeader("X-Webhook-Signature");

        // Buffer the body so we can both verify the HMAC and pass the
        // content down to the controller.
        byte[] body = StreamUtils.copyToByteArray(request.getInputStream());
        String bodyStr = new String(body, StandardCharsets.UTF_8);

        if (defaultSecret == null || defaultSecret.isBlank()) {
            reject(response, request, "Webhook secret not configured");
            return;
        }
        if (timestampHeader == null || signature == null) {
            reject(response, request, "Missing signature header");
            return;
        }
        long ts;
        try {
            ts = Long.parseLong(timestampHeader);
        } catch (NumberFormatException ex) {
            reject(response, request, "Bad timestamp header");
            return;
        }
        long now = System.currentTimeMillis() / 1000L;
        if (Math.abs(now - ts) > MAX_SKEW_SECONDS) {
            reject(response, request, "Timestamp outside allowed skew");
            return;
        }
        String expected = WebhookSignatureVerifier.compute(defaultSecret, ts, bodyStr);
        if (!WebhookSignatureVerifier.matches(expected, signature)) {
            reject(response, request, "Signature mismatch");
            return;
        }
        // Replay protection: identical signature in a short window -> reject
        Long seen = replayProtection.putIfAbsent(signature, System.currentTimeMillis());
        if (seen != null && (System.currentTimeMillis() - seen) < 60_000L) {
            reject(response, request, "Duplicate webhook within replay window");
            return;
        }
        // Periodically clean the replay cache
        if (replayProtection.size() > 10_000) {
            long cutoff = System.currentTimeMillis() - 60_000L;
            replayProtection.entrySet().removeIf(e -> e.getValue() < cutoff);
        }

        // Continue with a wrapper so the controller can still read the body.
        chain.doFilter(new CachedBodyRequest(request, body), response);
    }

    private void reject(HttpServletResponse response, HttpServletRequest request, String reason) throws IOException {
        try {
            com.hsf.hotel.admin.model.AuditLog entry = new com.hsf.hotel.admin.model.AuditLog();
            entry.setAction("WEBHOOK_REJECTED");
            entry.setEntityType("Webhook");
            entry.setIpAddress(ClientIpResolver.resolve(request));
            entry.setUserAgent(request.getHeader("User-Agent"));
            entry.setDetails(reason);
            auditLogRepository.save(entry);
        } catch (Exception ignored) {
            // Auditing must never break the response path.
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        java.util.LinkedHashMap<String, Object> error = new java.util.LinkedHashMap<>();
        error.put("code", ErrorCodes.UNAUTHORIZED);
        error.put("message", "Webhook signature verification failed");
        java.util.LinkedHashMap<String, Object> envelope = new java.util.LinkedHashMap<>();
        envelope.put("data", null);
        envelope.put("error", error);
        objectMapper.writeValue(response.getWriter(), envelope);
    }

    /** Wraps a request so the body can be re-read after buffering. */
    private static final class CachedBodyRequest extends jakarta.servlet.http.HttpServletRequestWrapper {
        private final byte[] body;

        CachedBodyRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public jakarta.servlet.ServletInputStream getInputStream() {
            java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(body);
            return new jakarta.servlet.ServletInputStream() {
                @Override public boolean isFinished() { return bis.available() == 0; }
                @Override public boolean isReady() { return true; }
                @Override public void setReadListener(jakarta.servlet.ReadListener listener) { /* not used */ }
                @Override public int read() { return bis.read(); }
            };
        }

        @Override
        public java.io.BufferedReader getReader() {
            return new java.io.BufferedReader(new java.io.InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }
}
