package com.hsf.hotel.it;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for integration tests against a real PostgreSQL 16 with the
 * production migration track ({@code db/postgresql}) and {@code ddl-auto=validate}
 * — the configuration H2-based tests can never exercise (ADR-0006).
 *
 * <p>Database source, in order of preference:
 * <ol>
 *   <li>{@code IT_JDBC_URL} / {@code IT_JDBC_USER} / {@code IT_JDBC_PASSWORD} environment
 *       variables — an externally managed, <b>empty, disposable</b> database
 *       (useful on machines without Docker);</li>
 *   <li>a Testcontainers {@code postgres:16-alpine} container (CI, developer laptops).</li>
 * </ol>
 * If neither is available the tests are skipped, never silently passed against H2.
 * <b>Every concrete subclass must carry {@link RequiresPostgres}</b> — JUnit condition
 * annotations are not inherited from superclasses.
 *
 * <p>Run with {@code ./mvnw verify} (failsafe picks up {@code *IT} classes).
 */
@SpringBootTest(properties = {
        "spring.main.lazy-initialization=false",
        "spring.datasource.driver-class-name=org.postgresql.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.open-in-view=false",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/postgresql",
        "spring.flyway.baseline-on-migrate=true",
        "spring.flyway.validate-on-migrate=true"
})
@ActiveProfiles("test")
public abstract class AbstractPostgresIT {

    private static final String EXTERNAL_URL = System.getenv("IT_JDBC_URL");

    private static PostgreSQLContainer<?> container;

    static boolean postgresAvailable() {
        if (EXTERNAL_URL != null && !EXTERNAL_URL.isBlank()) {
            return true;
        }
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable t) {
            return false;
        }
    }

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        if (EXTERNAL_URL != null && !EXTERNAL_URL.isBlank()) {
            registry.add("spring.datasource.url", () -> EXTERNAL_URL);
            registry.add("spring.datasource.username", () -> envOr("IT_JDBC_USER", "postgres"));
            registry.add("spring.datasource.password", () -> envOr("IT_JDBC_PASSWORD", ""));
            return;
        }
        synchronized (AbstractPostgresIT.class) {
            if (container == null) {
                container = new PostgreSQLContainer<>("postgres:16-alpine")
                        .withDatabaseName("hotel_it")
                        .withUsername("hotel_it")
                        .withPassword("hotel_it");
                container.start(); // reused by every IT class in this JVM; Ryuk removes it at exit
            }
        }
        registry.add("spring.datasource.url", container::getJdbcUrl);
        registry.add("spring.datasource.username", container::getUsername);
        registry.add("spring.datasource.password", container::getPassword);
    }

    private static String envOr(String key, String fallback) {
        String v = System.getenv(key);
        return v == null ? fallback : v;
    }
}
