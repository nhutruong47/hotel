package com.hsf.hotel.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Applies a strict baseline of HTTP response headers to every outbound
 * response. The values are conservative defaults; the production profile
 * tightens the HSTS max-age via the {@code app.security.hsts-max-age}
 * property.
 *
 * <p>Headers applied:
 * <ul>
 *   <li>{@code Content-Security-Policy} — restricts sources to self + API origin.</li>
 *   <li>{@code Strict-Transport-Security} — only when running over TLS.</li>
 *   <li>{@code X-Content-Type-Options: nosniff}</li>
 *   <li>{@code X-Frame-Options: DENY} — no embedding.</li>
 *   <li>{@code Referrer-Policy: strict-origin-when-cross-origin}</li>
 *   <li>{@code Permissions-Policy} — disables powerful features we do not need.</li>
 *   <li>{@code Cross-Origin-Opener-Policy: same-origin}</li>
 *   <li>{@code Cross-Origin-Resource-Policy: same-site}</li>
 *   <li>{@code Cache-Control: no-store} on authenticated API responses.</li>
 * </ul>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class SecurityHeadersFilter extends OncePerRequestFilter {

    private final long hstsMaxAgeSeconds;
    private final boolean tlsOnly;
    private final String apiOrigin;

    public SecurityHeadersFilter(
            @Value("${app.security.hsts-max-age:31536000}") long hstsMaxAgeSeconds,
            @Value("${app.security.tls-only:false}") boolean tlsOnly,
            @Value("${app.cors.allowed-origins:http://localhost:5173}") String apiOrigin) {
        this.hstsMaxAgeSeconds = hstsMaxAgeSeconds;
        this.tlsOnly = tlsOnly;
        this.apiOrigin = apiOrigin;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        applyCommonHeaders(response);
        applyApiHeaders(request, response);
        chain.doFilter(request, response);
    }

    private void applyCommonHeaders(HttpServletResponse response) {
        String csp = buildContentSecurityPolicy();
        response.setHeader(SecurityHeaderNames.CONTENT_SECURITY_POLICY, csp);
        response.setHeader(SecurityHeaderNames.X_CONTENT_TYPE_OPTIONS, "nosniff");
        response.setHeader(SecurityHeaderNames.X_FRAME_OPTIONS, "DENY");
        response.setHeader(SecurityHeaderNames.REFERRER_POLICY, "strict-origin-when-cross-origin");
        response.setHeader(SecurityHeaderNames.PERMISSIONS_POLICY,
                "camera=(), microphone=(), geolocation=(), payment=(), usb=(), magnetometer=(), gyroscope=()");
        response.setHeader(SecurityHeaderNames.CROSS_ORIGIN_OPENER_POLICY, "same-origin");
        response.setHeader(SecurityHeaderNames.CROSS_ORIGIN_RESOURCE_POLICY, "same-site");
        if (tlsOnly) {
            response.setHeader(SecurityHeaderNames.STRICT_TRANSPORT_SECURITY,
                    "max-age=" + hstsMaxAgeSeconds + "; includeSubDomains");
        }
    }

    private void applyApiHeaders(HttpServletRequest request, HttpServletResponse response) {
        String path = request.getRequestURI();
        if (path != null && path.startsWith("/api/")) {
            // Authenticated JSON responses should never be cached by intermediaries.
            response.setHeader(SecurityHeaderNames.CACHE_CONTROL, "no-store");
            response.setHeader(SecurityHeaderNames.PRAGMA, "no-cache");
        }
    }

    private String buildContentSecurityPolicy() {
        // The API itself serves JSON, so the CSP is enforced by the SPA. We
        // still set frame-ancestors 'none' and disallow object embedding to
        // mitigate clickjacking and Flash-style attacks against any HTML we
        // accidentally return.
        return "default-src 'self'; "
                + "frame-ancestors 'none'; "
                + "object-src 'none'; "
                + "base-uri 'self'; "
                + "form-action 'self'; "
                + "img-src 'self' data: https:; "
                + "script-src 'self'; "
                + "style-src 'self' 'unsafe-inline'; "
                + "connect-src 'self' " + apiOrigin + ";";
    }
}
