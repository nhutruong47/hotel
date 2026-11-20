package com.hsf.hotel.config;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

/** V13 exclusion violations (SQLSTATE 23P01) must map to 409 ROOM_UNAVAILABLE, not a generic CONFLICT. */
class BookingOverlapViolationMappingTest {

    @Test
    void detectsExclusionViolationBySqlState() {
        var ex = new DataIntegrityViolationException("could not execute statement",
                new SQLException("conflicting key value violates exclusion constraint", "23P01"));
        assertThat(GlobalExceptionHandler.isBookingOverlapViolation(ex)).isTrue();
    }

    @Test
    void detectsByConstraintName() {
        var ex = new DataIntegrityViolationException("x",
                new RuntimeException("violates exclusion constraint \"bookings_no_overlap\""));
        assertThat(GlobalExceptionHandler.isBookingOverlapViolation(ex)).isTrue();
    }

    @Test
    void ignoresOtherIntegrityViolations() {
        var ex = new DataIntegrityViolationException("dup",
                new SQLException("duplicate key value violates unique constraint", "23505"));
        assertThat(GlobalExceptionHandler.isBookingOverlapViolation(ex)).isFalse();
    }
}
