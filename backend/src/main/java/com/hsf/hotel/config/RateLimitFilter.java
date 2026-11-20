package com.hsf.hotel.config;

import com.hsf.hotel.security.ClientIpResolver;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Fixed-window rate limiter per client IP (single instance; a Redis-backed limiter
 * is planned with Spring Session in ADR-0005).
 *
 * <ul>
 *   <li><b>General</b>: every {@code /api/**} request —
 *       {@code app.security.rate-limit.requests} per window (default 60).</li>
 *   <li><b>Sensitive</b>: credential, account, AI and booking-creation endpoints
 *       (brute force, enumeration, inventory-hold abuse, paid AI calls) —
 *       {@code app.security.rate-limit.sensitive-requests} per window (default 10).</li>
 * </ul>
 * Static files and health probes are not limited. Expired windows are evicted
 * periodically so the map cannot grow without bound.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    /** POST endpoints that get the stricter per-IP budget. */
    static final Set<String> SENSITIVE_POST_PATHS = Set.of(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/auth/resend-verification",
            "/api/v1/ai/recommend",
            "/api/v1/contact",
            "/api/v1/bookings"
    );

    private static final int EVICT_EVERY_N_REQUESTS = 1_000;

    private final int maxRequests;
    private final int maxSensitiveRequests;
    private final long windowMs;
    private final Map<String, RateLimitEntry> buckets = new ConcurrentHashMap<>();
    private final AtomicLong requestCounter = new AtomicLong();

    public RateLimitFilter(
            @Value("${app.security.rate-limit.requests:60}") int maxRequests,
            @Value("${app.security.rate-limit.sensitive-requests:10}") int maxSensitiveRequests,
            @Value("${app.security.rate-limit.window-ms:60000}") long windowMs) {
        this.maxRequests = maxRequests;
        this.maxSensitiveRequests = maxSensitiveRequests;
        this.windowMs = windowMs;
        log.info("Rate limiter initialized: {} requests ({} sensitive) per {} ms",
                maxRequests, maxSensitiveRequests, windowMs);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String path = httpRequest.getRequestURI();

        if (path == null || !path.startsWith("/api/") || path.startsWith("/api/v1/health")) {
            chain.doFilter(request, response);
            return;
        }
        maybeEvict();

        String clientIp = ClientIpResolver.resolve(httpRequest);
        if (!tryAcquire("g|" + clientIp, maxRequests)) {
            reject(httpResponse, clientIp, path, maxRequests);
            return;
        }
        if (isSensitive(httpRequest.getMethod(), path)
                && !tryAcquire("s|" + clientIp, maxSensitiveRequests)) {
            reject(httpResponse, clientIp, path, maxSensitiveRequests);
            return;
        }
        chain.doFilter(request, response);
    }

    static boolean isSensitive(String method, String path) {
        return "POST".equalsIgnoreCase(method) && SENSITIVE_POST_PATHS.contains(path);
    }

    private boolean tryAcquire(String key, int limit) {
        long now = Instant.now().toEpochMilli();
        RateLimitEntry entry = buckets.compute(key, (k, current) ->
                current == null || current.windowStart <= now - windowMs
                        ? new RateLimitEntry(now)
                        : current);
        return entry.count.incrementAndGet() <= limit;
    }

    private void maybeEvict() {
        if (requestCounter.incrementAndGet() % EVICT_EVERY_N_REQUESTS != 0) {
            return;
        }
        long cutoff = Instant.now().toEpochMilli() - windowMs;
        buckets.entrySet().removeIf(e -> e.getValue().windowStart <= cutoff);
    }

    int trackedKeys() {
        return buckets.size();
    }

    private void reject(HttpServletResponse response, String clientIp, String path, int limit) throws IOException {
        log.warn("Rate limit exceeded for IP: {} on path: {}", clientIp, path);
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Retry-After", String.valueOf(Math.max(1, windowMs / 1000)));
        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", "0");
        response.getWriter().write("""
            {
                "data": null,
                "error": {
                    "code": "RATE_LIMIT_EXCEEDED",
                    "message": "Quá nhiều yêu cầu. Vui lòng thử lại sau."
                }
            }
            """);
    }

    @Override
    public void destroy() {
        buckets.clear();
    }

    private static final class RateLimitEntry {
        final long windowStart;
        final AtomicInteger count = new AtomicInteger();

        RateLimitEntry(long windowStart) {
            this.windowStart = windowStart;
        }
    }
}
