package com.hsf.hotel.it;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The application context only starts if every Flyway migration in
 * {@code db/postgresql} applies cleanly AND Hibernate {@code ddl-auto=validate}
 * accepts the resulting schema — i.e. exactly what a production deploy does.
 */
@DisplayName("PostgreSQL migrations + Hibernate schema validation")
@RequiresPostgres
class PostgresMigrationIT extends AbstractPostgresIT {

    @Autowired private JdbcTemplate jdbc;

    @Test
    void allMigrationsAppliedSuccessfully() {
        Integer failed = jdbc.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE success = false", Integer.class);
        assertThat(failed).isZero();

        String latest = jdbc.queryForObject(
                "SELECT version FROM flyway_schema_history WHERE success AND version IS NOT NULL "
                        + "ORDER BY installed_rank DESC LIMIT 1", String.class);
        assertThat(latest).isEqualTo("14");
    }

    @Test
    void manualRefundWorkflowSchemaExists() {
        String check = jdbc.queryForObject(
                "SELECT pg_get_constraintdef(oid) FROM pg_constraint WHERE conname = 'chk_payment_refund_status'",
                String.class);
        assertThat(check).contains("PENDING_MANUAL");
        Integer reviewColumn = jdbc.queryForObject(
                "SELECT count(*) FROM information_schema.columns "
                        + "WHERE table_name = 'payments' AND column_name = 'review_required'", Integer.class);
        assertThat(reviewColumn).isEqualTo(1);
    }

    @Test
    void bookingIntegrityConstraintsExist() {
        Integer exclusion = jdbc.queryForObject(
                "SELECT count(*) FROM pg_constraint WHERE conname = 'bookings_no_overlap' AND contype = 'x'",
                Integer.class);
        Integer dateCheck = jdbc.queryForObject(
                "SELECT count(*) FROM pg_constraint WHERE conname = 'chk_bookings_date_order' AND contype = 'c'",
                Integer.class);
        Integer extension = jdbc.queryForObject(
                "SELECT count(*) FROM pg_extension WHERE extname = 'btree_gist'", Integer.class);
        assertThat(exclusion).isEqualTo(1);
        assertThat(dateCheck).isEqualTo(1);
        assertThat(extension).isEqualTo(1);
    }
}
