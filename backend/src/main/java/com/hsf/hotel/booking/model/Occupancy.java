package com.hsf.hotel.booking.model;

import java.time.LocalDate;

/**
 * Single source of truth for how a booking occupies inventory.
 *
 * <p>Overnight stays occupy the half-open range {@code [checkIn, checkOut)}.
 * A day-use booking ({@code checkIn == checkOut}, priced at 50% of one night)
 * would be an <em>empty</em> range under that rule and silently overlap with
 * every other booking — the double-booking defect P0-02. Day-use therefore
 * occupies the whole calendar day: {@code [checkIn, checkIn + 1)}.
 *
 * <p>The same rule is enforced in PostgreSQL by the exclusion constraint
 * {@code bookings_no_overlap} (V13): {@code daterange(check_in_date,
 * GREATEST(check_out_date, check_in_date + 1), '[)')}.
 */
public final class Occupancy {

    private Occupancy() {}

    /** Exclusive end date of the occupied range for a requested stay. */
    public static LocalDate exclusiveEnd(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null) {
            return checkOut;
        }
        return checkOut.isAfter(checkIn) ? checkOut : checkIn.plusDays(1);
    }

    /**
     * JPQL predicate fragment: booking alias {@code b} overlaps
     * {@code [:checkIn, :checkOut)} where {@code :checkOut} is already an
     * exclusive end computed by {@link #exclusiveEnd}. Existing day-use rows
     * ({@code checkOutDate = checkInDate}) are treated as {@code [d, d+1)}.
     */
    public static final String OVERLAP_JPQL =
            "(b.checkInDate < :checkOut AND (b.checkOutDate > :checkIn "
                    + "OR (b.checkOutDate = b.checkInDate AND b.checkInDate >= :checkIn)))";
}
