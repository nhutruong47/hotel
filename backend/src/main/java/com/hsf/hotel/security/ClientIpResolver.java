package com.hsf.hotel.security;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Set;

/**
 * Resolves the originating client IP for an HTTP request, respecting the
 * configured proxy trust list. Centralised here so the rate limiter, the
 * audit log service, and the request id filter all agree on the source.
 */
public final class ClientIpResolver {

    private static final Set<String> LOCAL_FALLBACKS = Set.of("0:0:0:0:0:0:0:1", "127.0.0.1", "::1");

    private ClientIpResolver() {}

    /**
     * Returns the client IP. When the application is behind a reverse proxy
     * the {@code X-Forwarded-For} header is consulted, but only the first
     * segment is used and it is validated against a permissive syntax check
     * to defeat header injection.
     */
    public static String resolve(HttpServletRequest request) {
        String remote = request.getRemoteAddr();
        String forwarded = request.getHeader("X-Forwarded-For");
        if (isTrustedProxy(remote) && forwarded != null && !forwarded.isBlank()) {
            String first = forwarded.split(",", 2)[0].trim();
            if (isValidIp(first)) {
                return first;
            }
        }
        return remote == null ? "unknown" : remote;
    }

    private static boolean isValidIp(String candidate) {
        if (candidate == null || candidate.isEmpty() || candidate.length() > 64) {
            return false;
        }
        for (int i = 0; i < candidate.length(); i++) {
            char c = candidate.charAt(i);
            if (c == '.' || c == ':' || c == '-' || c >= '0' && c <= '9'
                    || c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F') {
                continue;
            }
            return false;
        }
        return true;
    }

    /** Convenience for places that only need to know if the call is local. */
    public static boolean isLocal(HttpServletRequest request) {
        String ip = resolve(request);
        return LOCAL_FALLBACKS.contains(ip);
    }

    private static boolean isTrustedProxy(String remote) {
        if (remote == null || remote.isBlank()) {
            return false;
        }
        if (LOCAL_FALLBACKS.contains(remote)) {
            return true;
        }
        return remote.startsWith("10.")
                || remote.startsWith("192.168.")
                || is172Private(remote)
                || remote.startsWith("fc")
                || remote.startsWith("fd");
    }

    private static boolean is172Private(String remote) {
        if (!remote.startsWith("172.")) {
            return false;
        }
        String[] parts = remote.split("\\.");
        if (parts.length < 2) {
            return false;
        }
        try {
            int second = Integer.parseInt(parts[1]);
            return second >= 16 && second <= 31;
        } catch (NumberFormatException ex) {
            return false;
        }
    }
}
