package com.hsf.hotel.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utilities for handling opaque security tokens (email verification, password
 * reset, CSRF). Tokens are stored only as a SHA-256 hash so a database leak
 * does not immediately expose valid credentials.
 */
public final class TokenHasher {

    private static final SecureRandom RNG = new SecureRandom();

    private TokenHasher() {}

    /** Generates a URL-safe random token of the requested size in bytes. */
    public static String generateToken(int bytes) {
        byte[] buffer = new byte[bytes];
        RNG.nextBytes(buffer);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer);
    }

    /** Hashes a token with SHA-256 and returns the hex digest. */
    public static String hash(String token) {
        if (token == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /** Constant-time equality check between a raw token and a stored hash. */
    public static boolean matches(String rawToken, String storedHash) {
        if (rawToken == null || storedHash == null) {
            return false;
        }
        String computed = hash(rawToken);
        return MessageDigest.isEqual(
                computed.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                storedHash.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
