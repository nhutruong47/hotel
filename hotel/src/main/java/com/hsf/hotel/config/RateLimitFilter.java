package com.hsf.hotel.config;

import com.hsf.hotel.exception.RateLimitExceededException;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate limiting filter to prevent brute-force attacks and API abuse.
 * 
 * <p>Implements a sliding window rate limiter per IP address.
 * Limits are configurable via application properties:
 * <ul>
 *   <li>{@code app.security.rate-limit.requests} - max requests per window (default: 60)</li>
 *   <li>{@code app.security.rate-limit.window-ms} - window size in milliseconds (default: 60000)</li>
 * </ul>
 * 
 * <p>Excluded paths:
 * <ul>
 *   <li>/actuator/health - health checks</li>
 *   <li>/api/v1/health - API health endpoint</li>
 * </ul>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final int maxRequests;
    private final long windowMs;
    private final Map<String, RateLimitEntry> rateLimitMap = new ConcurrentHashMap<>();

    public RateLimitFilter(
            @Value("${app.security.rate-limit.requests:60}") int maxRequests,
            @Value("${app.security.rate-limit.window-ms:60000}") long windowMs) {
        this.maxRequests = maxRequests;
        this.windowMs = windowMs;
        log.info("Rate limiter initialized: {} requests per {} ms", maxRequests, windowMs);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getRequestURI();

        // Skip rate limiting for health checks
        if (isExcludedPath(path)) {
            chain.doFilter(request, response);
            return;
        }

        String clientIp = ClientIpResolver.resolve(httpRequest);

        if (!isAllowed(clientIp)) {
            log.warn("Rate limit exceeded for IP: {} on path: {}", clientIp, path);
            sendRateLimitResponse(httpResponse);
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isExcludedPath(String path) {
        return path.startsWith("/actuator/health")
                || path.startsWith("/api/v1/health");
    }

    private boolean isAllowed(String clientIp) {
        long now = Instant.now().toEpochMilli();
        long windowStart = now - windowMs;

        rateLimitMap.compute(clientIp, (ip, entry) -> {
            if (entry == null || entry.windowStart < windowStart) {
                // Start new window
                return new RateLimitEntry(now, new AtomicInteger(1));
            }

            // Increment counter in current window
            entry.count.incrementAndGet();

            // Check if limit exceeded
            if (entry.count.get() > maxRequests) {
                return entry; // Return unchanged, will be rejected
            }

            return entry;
        });

        RateLimitEntry entry = rateLimitMap.get(clientIp);
        if (entry == null) {
            return true;
        }

        return entry.count.get() <= maxRequests;
    }

    private void sendRateLimitResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", String.valueOf(windowMs / 1000));
        response.setHeader("X-RateLimit-Limit", String.valueOf(maxRequests));
        response.setHeader("X-RateLimit-Remaining", "0");

        String json = """
            {
                "data": null,
                "error": {
                    "code": "RATE_LIMIT_EXCEEDED",
                    "message": "Quá nhiều yêu cầu. Vui lòng thử lại sau."
                }
            }
            """;

        response.getWriter().write(json);
    }

    @Override
    public void destroy() {
        rateLimitMap.clear();
    }

    /**
     * Internal class to track rate limit state per client.
     */
    private static class RateLimitEntry {
        final long windowStart;
        final AtomicInteger count;

        RateLimitEntry(long windowStart, AtomicInteger count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}
