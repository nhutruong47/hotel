-- Manual rollback for V16. applied_voucher_code was kept, so no information is lost.
BEGIN;
ALTER TABLE bookings DROP CONSTRAINT IF EXISTS fk_bookings_approved_by;
ALTER TABLE bookings DROP CONSTRAINT IF EXISTS fk_bookings_promotion;
ALTER TABLE bookings DROP CONSTRAINT IF EXISTS fk_bookings_voucher;
DROP INDEX IF EXISTS idx_bookings_voucher;
DROP INDEX IF EXISTS idx_bookings_promotion;
ALTER TABLE bookings DROP COLUMN IF EXISTS promotion_id;
ALTER TABLE bookings DROP COLUMN IF EXISTS voucher_id;
DELETE FROM flyway_schema_history WHERE version = '16';
COMMIT;