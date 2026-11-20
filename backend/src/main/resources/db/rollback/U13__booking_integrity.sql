-- =============================================================================
-- Manual rollback for V13__booking_integrity.sql
-- NOT a Flyway location (Flyway Community has no undo). Run by hand with psql:
--   psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f U13__booking_integrity.sql
-- Data-preserving: only drops constraints. See docs/architecture/database-design.md §3.3
-- =============================================================================
BEGIN;
ALTER TABLE bookings DROP CONSTRAINT IF EXISTS bookings_no_overlap;
ALTER TABLE bookings DROP CONSTRAINT IF EXISTS chk_bookings_date_order;
-- Keep the btree_gist extension: harmless, and may be used by other objects.
DELETE FROM flyway_schema_history WHERE version = '13';
COMMIT;
