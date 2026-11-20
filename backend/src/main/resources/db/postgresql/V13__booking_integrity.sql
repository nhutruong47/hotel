-- =============================================================================
-- V13 — Booking integrity: database-level guarantee against double booking
-- -----------------------------------------------------------------------------
-- Context  : Double booking was prevented only in application code
--            (PESSIMISTIC_WRITE lock on the room + overlap query). Any code path
--            that bypasses BookingService (admin tooling, manual SQL, a future
--            module, a bug such as P0-02 zero-night stays) could still insert an
--            overlapping booking. See docs/adr/0003-* and docs/architecture/database-design.md.
-- Semantics: overnight stay occupies [check_in, check_out); a day-use booking
--            (check_in = check_out) occupies the whole day [d, d+1). This mirrors
--            com.hsf.hotel.booking.model.Occupancy exactly.
--            CANCELLED / EXPIRED / NO_SHOW release inventory and are excluded.
-- Safety   : The migration never modifies booking data. If existing rows violate
--            the new rules it aborts with the offending ids so an operator can
--            resolve them deliberately (forward-only policy, no silent fixes).
-- Rollback : see "V13 rollback" in docs/architecture/database-design.md
-- =============================================================================

-- btree_gist lets a GiST exclusion constraint combine "=" on room_id with "&&"
-- on a date range. Trusted extension since PostgreSQL 13 (DB owner may create it).
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- ---------------------------------------------------------------------------
-- 1. Pre-flight: refuse to continue if current data already violates the rules
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    bad_dates   TEXT;
    overlap_pairs    TEXT;
BEGIN
    SELECT string_agg(id::TEXT, ', ' ORDER BY id)
      INTO bad_dates
      FROM bookings
     WHERE check_in_date IS NULL
        OR check_out_date IS NULL
        OR check_out_date < check_in_date;

    IF bad_dates IS NOT NULL THEN
        RAISE EXCEPTION 'V13 aborted: bookings with missing or inverted dates: %', bad_dates
            USING HINT = 'Fix or cancel these bookings manually, then re-run the migration.';
    END IF;

    SELECT string_agg(a.id::TEXT || '<->' || b.id::TEXT, ', ' ORDER BY a.id, b.id)
      INTO overlap_pairs
      FROM bookings a
      JOIN bookings b
        ON a.room_id = b.room_id
       AND a.id < b.id
       AND daterange(a.check_in_date, GREATEST(a.check_out_date, a.check_in_date + 1), '[)')
        && daterange(b.check_in_date, GREATEST(b.check_out_date, b.check_in_date + 1), '[)')
     WHERE a.status NOT IN ('CANCELLED', 'EXPIRED', 'NO_SHOW')
       AND b.status NOT IN ('CANCELLED', 'EXPIRED', 'NO_SHOW');

    IF overlap_pairs IS NOT NULL THEN
        RAISE EXCEPTION 'V13 aborted: overlapping active bookings (same room): %', overlap_pairs
            USING HINT = 'Resolve each pair (cancel / move one booking) before migrating.';
    END IF;
END $$;

-- ---------------------------------------------------------------------------
-- 2. Row-level rules
-- ---------------------------------------------------------------------------
ALTER TABLE bookings DROP CONSTRAINT IF EXISTS chk_bookings_date_order;
ALTER TABLE bookings ADD CONSTRAINT chk_bookings_date_order
    CHECK (check_out_date >= check_in_date);

-- ---------------------------------------------------------------------------
-- 3. Cross-row rule: no two active bookings of the same room may overlap
-- ---------------------------------------------------------------------------
ALTER TABLE bookings DROP CONSTRAINT IF EXISTS bookings_no_overlap;
ALTER TABLE bookings ADD CONSTRAINT bookings_no_overlap
    EXCLUDE USING gist (
        room_id WITH =,
        daterange(check_in_date, GREATEST(check_out_date, check_in_date + 1), '[)') WITH &&
    )
    WHERE (status NOT IN ('CANCELLED', 'EXPIRED', 'NO_SHOW'));

COMMENT ON CONSTRAINT bookings_no_overlap ON bookings IS
    'P0 double-booking guard. Mirrors com.hsf.hotel.booking.model.Occupancy. Violations surface as SQLSTATE 23P01 -> HTTP 409 ROOM_UNAVAILABLE.';
