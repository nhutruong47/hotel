package com.hsf.hotel.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Lightweight CSRF defence for cookie-authenticated state-changing requests.
 *
 * <p>For every request the filter ensures a non-readable-by-JS cookie
 * {@code XSRF-TOKEN} is present. State-changing requests (anything other
 * than {@code GET}, {@code HEAD}, {@code OPTIONS}) are required to echo the
 * same value in either a request header ({@code X-XSRF-TOKEN}) or form
 * parameter ({@code _csrf}). Comparison is constant-time.
 *
 * <p>This deliberately avoids the Spring Security CSRF repository so the
 * existing session-bridge filter keeps working untouched.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class CsrfCookieFilter extends OncePerRequestFilter {

    public static final String COOKIE_NAME = "XSRF-TOKEN";
    public static final String HEADER_NAME = "X-XSRF-TOKEN";
    public static final String PARAM_NAME = "_csrf";

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    private static final Set<String> CSRF_EXEMPT_PATHS = Set.of(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/auth/verify",
            "/api/v1/auth/resend-verification",
            "/api/v1/payments/webhook" // gateway signature is the real auth
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        boolean secure = request.isSecure();
        Cookie csrfCookie = ensureCookie(request, response, secure);

        if (SAFE_METHODS.contains(request.getMethod().toUpperCase()) || isExempt(request.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }

        String supplied = request.getHeader(HEADER_NAME);
        if (supplied == null || supplied.isBlank()) {
            supplied = request.getParameter(PARAM_NAME);
        }

        if (supplied == null || !constantTimeEquals(supplied, csrfCookie.value)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"data\":null,\"error\":{\"code\":\"CSRF_TOKEN_INVALID\","
                            + "\"message\":\"CSRF token missing or invalid\"}}");
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isExempt(String path) {
        if (path == null) return false;
        for (String exempt : CSRF_EXEMPT_PATHS) {
            if (path.startsWith(exempt)) return true;
        }
        return false;
    }

    private Cookie ensureCookie(HttpServletRequest request, HttpServletResponse response, boolean secure) {
        Cookie existing = readCookie(request);
        if (existing != null) {
            return existing;
        }
        String token = TokenHasher.generateToken(24);
        Cookie cookie = new Cookie(token);
        response.addHeader("Set-Cookie",
                COOKIE_NAME + "=" + token
                        + "; Path=/; HttpOnly=false; SameSite=Strict"
                        + (secure ? "; Secure" : "")
                        + "; Max-Age=86400");
        return cookie;
    }

    private Cookie readCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (jakarta.servlet.http.Cookie c : request.getCookies()) {
            if (COOKIE_NAME.equals(c.getName())) {
                return new Cookie(c.getValue());
            }
        }
        return null;
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) return false;
        int diff = 0;
        for (int i = 0; i < a.length(); i++) {
            diff |= a.charAt(i) ^ b.charAt(i);
        }
        return diff == 0;
    }

    /** Local value object to avoid pulling Servlet Cookie into signatures. */
    private record Cookie(String value) {}
}
