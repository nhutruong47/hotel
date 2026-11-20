-- =============================================================================
-- V16 — Booking discount references (expand step) + approver FK
-- -----------------------------------------------------------------------------
-- bookings.applied_voucher_code holds EITHER a voucher code or a promotion promo
-- code, so it cannot carry a foreign key. Expand: add voucher_id / promotion_id
-- with real FKs, backfill them from the code, keep applied_voucher_code for
-- compatibility (contract step in a later release). The application now writes
-- both (BookingService#createBooking).
-- bookings.approved_by had an index but no FK.
-- Rollback: db/rollback/U16__booking_discount_refs.sql
-- =============================================================================

DO $$
DECLARE
    orphans TEXT;
BEGIN
    SELECT string_agg(b.id::TEXT || '->' || b.approved_by::TEXT, ', ' ORDER BY b.id)
      INTO orphans
      FROM bookings b
     WHERE b.approved_by IS NOT NULL
       AND NOT EXISTS (SELECT 1 FROM users u WHERE u.id = b.approved_by);
    IF orphans IS NOT NULL THEN
        RAISE EXCEPTION 'V16 aborted: bookings.approved_by points to missing users (booking->user): %', orphans
            USING HINT = 'Set approved_by to NULL or to an existing user for these bookings, then re-run.';
    END IF;
END $$;

ALTER TABLE bookings ADD COLUMN IF NOT EXISTS voucher_id BIGINT;
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS promotion_id BIGINT;

ALTER TABLE bookings DROP CONSTRAINT IF EXISTS fk_bookings_voucher;
ALTER TABLE bookings ADD CONSTRAINT fk_bookings_voucher
    FOREIGN KEY (voucher_id) REFERENCES vouchers (id) ON DELETE SET NULL;
ALTER TABLE bookings DROP CONSTRAINT IF EXISTS fk_bookings_promotion;
ALTER TABLE bookings ADD CONSTRAINT fk_bookings_promotion
    FOREIGN KEY (promotion_id) REFERENCES promotions (id) ON DELETE SET NULL;
ALTER TABLE bookings DROP CONSTRAINT IF EXISTS fk_bookings_approved_by;
ALTER TABLE bookings ADD CONSTRAINT fk_bookings_approved_by
    FOREIGN KEY (approved_by) REFERENCES users (id);

-- Backfill (codes are matched case-insensitively, as the application does).
UPDATE bookings b
   SET voucher_id = v.id
  FROM vouchers v
 WHERE b.voucher_id IS NULL
   AND b.applied_voucher_code IS NOT NULL
   AND UPPER(v.code) = UPPER(b.applied_voucher_code);

UPDATE bookings b
   SET promotion_id = p.id
  FROM (SELECT DISTINCT ON (UPPER(promo_code)) id, promo_code
          FROM promotions
         WHERE promo_code IS NOT NULL
         ORDER BY UPPER(promo_code), id) p
 WHERE b.promotion_id IS NULL
   AND b.voucher_id IS NULL
   AND b.applied_voucher_code IS NOT NULL
   AND UPPER(p.promo_code) = UPPER(b.applied_voucher_code);

CREATE INDEX IF NOT EXISTS idx_bookings_voucher   ON bookings (voucher_id);
CREATE INDEX IF NOT EXISTS idx_bookings_promotion ON bookings (promotion_id);
