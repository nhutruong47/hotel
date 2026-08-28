package com.hsf.hotel.security;

import java.util.regex.Pattern;

/**
 * Defensive helpers that sanitise user-controlled strings before they reach
 * logs, error messages, or {@code LIKE} queries.
 */
public final class Sanitizers {

    private static final Pattern CONTROL_CHARS = Pattern.compile("[\\p{Cntrl}&&[^\\r\\n\\t]]");
    private static final Pattern LIKE_META = Pattern.compile("([%_\\\\])");
    private static final int MAX_INPUT_LOG = 256;

    private Sanitizers() {}

    /**
     * Strips non-printable control characters (excluding CR/LF/TAB) and clips
     * the input to a safe length. Used before echoing user input in logs or
     * error messages to mitigate log injection / response splitting.
     */
    public static String safeForLog(String input) {
        if (input == null) return "";
        String cleaned = CONTROL_CHARS.matcher(input).replaceAll("");
        if (cleaned.length() > MAX_INPUT_LOG) {
            cleaned = cleaned.substring(0, MAX_INPUT_LOG) + "…";
        }
        return cleaned;
    }

    /**
     * Escapes characters that have special meaning in SQL {@code LIKE}
     * patterns. The {@code \\} escape character must be declared via
     * {@code JPA CriteriaBuilder.like(value, pattern, '\\')} or {@code ESCAPE}
     * in JPQL.
     */
    public static String escapeLikePattern(String input) {
        if (input == null) return "";
        return LIKE_META.matcher(input).replaceAll("\\\\$1");
    }

    /**
     * Minimal HTML escape for places where user-supplied content is rendered
     * into an HTML response (error pages, PDFs that read from user input,
     * etc.).
     */
    public static String htmlEscape(String input) {
        if (input == null) return "";
        StringBuilder out = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&#39;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }
}
