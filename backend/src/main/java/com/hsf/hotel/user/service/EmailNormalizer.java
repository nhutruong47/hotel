package com.hsf.hotel.user.service;

import java.util.Locale;

/** Canonical representation used for every account-email lookup and write. */
public final class EmailNormalizer {
    private EmailNormalizer() {}

    public static String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
