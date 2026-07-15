package com.hsf.hotel.observability;

import com.hsf.hotel.security.Sanitizers;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Tags every incoming request with a correlation id. The id is read from
 * the standard {@code X-Request-Id} header when present, otherwise a new
 * UUID is minted. The value is exposed in the response header so clients
 * can attach it to bug reports, and pushed into the MDC so every log line
 * produced within the request automatically carries it.
 *
 * <p>The incoming header is validated against a strict whitelist to
 * prevent log injection and response splitting attacks. Anything outside
 * the allowed character set is discarded and a fresh UUID is minted.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-Id";
    public static final String MDC_KEY = "requestId";

    private static final int MAX_LENGTH = 64;
    private static final Pattern ALLOWED = Pattern.compile("[A-Za-z0-9._\\-]+");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestId = sanitise(request.getHeader(HEADER));
        MDC.put(MDC_KEY, requestId);
        response.setHeader(HEADER, requestId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    private static String sanitise(String incoming) {
        if (incoming == null) {
            return UUID.randomUUID().toString();
        }
        String trimmed = incoming.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_LENGTH || !ALLOWED.matcher(trimmed).matches()) {
            return UUID.randomUUID().toString();
        }
        // Defense-in-depth: also strip any control chars the regex may have
        // missed (defensive copy in case of regex-engine quirks).
        return Sanitizers.safeForLog(trimmed);
    }
}
