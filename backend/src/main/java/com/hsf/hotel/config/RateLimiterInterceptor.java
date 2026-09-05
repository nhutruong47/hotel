package com.hsf.hotel.config;

import com.hsf.hotel.security.ClientIpResolver;
import com.hsf.hotel.security.SecurityHeaderNames;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Per-IP token-bucket rate limiter for the {@code /api/**} tree. The default
 * budget of 60 requests/minute applies to most endpoints; higher-traffic
 * public reads (rooms, blogs) are excluded so a single crawler cannot
 * starve a real user.
 *
 * <p>Security headers that previously lived here were moved to
 * {@link com.hsf.hotel.security.SecurityHeadersFilter} so the policy lives
 * in exactly one place.
 */
@Component
public class RateLimiterInterceptor implements HandlerInterceptor {

    /** Default budget for most API endpoints. */
    private static final int DEFAULT_MAX_REQUESTS = 60;
    private static final long DEFAULT_WINDOW_MS = 60_000L;

    /** Stricter budget for authentication and password endpoints. */
    private static final int AUTH_MAX_REQUESTS = 10;
    private static final long AUTH_WINDOW_MS = 60_000L;

    private static final Map<String, Integer> MAX_REQUESTS_OVERRIDE = Map.of();
    private static final java.util.Set<String> SKIP_PATHS = java.util.Set.of(
            "/api/v1/health", "/actuator/health", "/actuator/info"
    );
    private static final java.util.Set<String> STRICT_PREFIXES = java.util.Set.of(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/auth/resend-verification"
    );

    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private com.hsf.hotel.admin.service.AuditLogService auditLogService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        if (path != null && SKIP_PATHS.contains(path)) {
            return true;
        }
        if (path != null && STRICT_PREFIXES.stream().anyMatch(path::startsWith)) {
            return consume(request, response, AUTH_MAX_REQUESTS, AUTH_WINDOW_MS);
        }
        if (path != null && MAX_REQUESTS_OVERRIDE.containsKey(path)) {
            return consume(request, response, MAX_REQUESTS_OVERRIDE.get(path), DEFAULT_WINDOW_MS);
        }
        return consume(request, response, DEFAULT_MAX_REQUESTS, DEFAULT_WINDOW_MS);
    }

    private boolean consume(HttpServletRequest request, HttpServletResponse response, int max, long windowMs) throws java.io.IOException {
        String clientIp = ClientIpResolver.resolve(request);
        long now = System.currentTimeMillis();

        TokenBucket bucket = buckets.compute(clientIp, (key, existing) -> {
            if (existing == null || now - existing.windowStart.get() > windowMs) {
                return new TokenBucket(new AtomicLong(now), new AtomicInteger(1));
            }
            existing.count.incrementAndGet();
            return existing;
        });

        if (bucket.count.get() > max) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(Math.max(1, windowMs / 1000)));
            response.setContentType("application/json;charset=UTF-8");
            response.setHeader(SecurityHeaderNames.CACHE_CONTROL, "no-store");
            response.getWriter().write(
                    "{\"data\":null,\"error\":{\"code\":\"RATE_LIMIT_EXCEEDED\","
                            + "\"message\":\"Too many requests. Please try again later.\"}}");
            if (auditLogService != null) {
                try {
                    auditLogService.log(null, com.hsf.hotel.security.AuditActions.RATE_LIMIT_EXCEEDED,
                            "Request", null,
                            "rate limit hit for path " + request.getRequestURI(),
                            request);
                } catch (Exception ignored) {
                    // Auditing must never break the request pipeline.
                }
            }
            return false;
        }
        return true;
    }

    private static class TokenBucket {
        final AtomicLong windowStart;
        final AtomicInteger count;

        TokenBucket(AtomicLong windowStart, AtomicInteger count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}
