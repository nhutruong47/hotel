package com.hsf.hotel.config;

/**
 * Canonical API namespaces. Controllers and security rules must use these
 * constants so versioning cannot drift between routing and authorization.
 */
public final class ApiPaths {
    public static final String ROOT = "/api";
    public static final String VERSION = "v1";
    public static final String V1 = ROOT + "/" + VERSION;

    private ApiPaths() {
    }
}
