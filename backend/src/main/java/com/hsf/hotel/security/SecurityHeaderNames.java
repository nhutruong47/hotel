package com.hsf.hotel.security;

import org.springframework.http.HttpHeaders;

/**
 * Names of HTTP response headers set by the security layer. Centralised so
 * the policy cannot drift between the filter chain, the rate limiter, and
 * future integrations.
 */
public final class SecurityHeaderNames {

    private SecurityHeaderNames() {}

    public static final String CONTENT_SECURITY_POLICY = "Content-Security-Policy";
    public static final String STRICT_TRANSPORT_SECURITY = "Strict-Transport-Security";
    public static final String X_CONTENT_TYPE_OPTIONS = "X-Content-Type-Options";
    public static final String X_FRAME_OPTIONS = "X-Frame-Options";
    public static final String REFERRER_POLICY = "Referrer-Policy";
    public static final String PERMISSIONS_POLICY = "Permissions-Policy";
    public static final String CROSS_ORIGIN_OPENER_POLICY = "Cross-Origin-Opener-Policy";
    public static final String CROSS_ORIGIN_RESOURCE_POLICY = "Cross-Origin-Resource-Policy";
    public static final String CACHE_CONTROL = HttpHeaders.CACHE_CONTROL;
    public static final String PRAGMA = HttpHeaders.PRAGMA;
}
