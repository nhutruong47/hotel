-- =============================================================================
-- V15 — Room integrity + missing foreign-key indexes (P2, database-design.md §4)
-- -----------------------------------------------------------------------------
-- * rooms.slug is used in public URLs (/villas/{slug}) and looked up with
--   findBySlug(): it must be unique (the entity already declares unique=true).
-- * rooms.version backs optimistic locking (@Version); NULL breaks it.
-- * Foreign-key columns without an index make deletes/joins scan whole tables.
-- Pre-flight aborts (listing the rows) instead of rewriting business data.
-- The only data change is the technical backfill rooms.version NULL -> 0.
-- Rollback: db/rollback/U15__rooms_integrity.sql
-- =============================================================================

DO $$
DECLARE
    dup_slugs TEXT;
BEGIN
    SELECT string_agg(slug || ' (rooms ' || ids || ')', '; ')
      INTO dup_slugs
      FROM (SELECT slug, string_agg(id::TEXT, ',' ORDER BY id) AS ids
              FROM rooms
             WHERE slug IS NOT NULL
             GROUP BY slug
            HAVING count(*) > 1) d;
    IF dup_slugs IS NOT NULL THEN
        RAISE EXCEPTION 'V15 aborted: duplicate room slugs: %', dup_slugs
            USING HINT = 'Give each villa a distinct slug, then re-run.';
    END IF;
END $$;

ALTER TABLE rooms DROP CONSTRAINT IF EXISTS uq_rooms_slug;
ALTER TABLE rooms ADD CONSTRAINT uq_rooms_slug UNIQUE (slug);

UPDATE rooms SET version = 0 WHERE version IS NULL;
ALTER TABLE rooms ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE rooms ALTER COLUMN version SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_room_gallery_images_room ON room_gallery_images (room_id);
CREATE INDEX IF NOT EXISTS idx_review_photos_review     ON review_photos (review_id);
CREATE INDEX IF NOT EXISTS idx_promotion_rooms_room     ON promotion_rooms (room_id);
CREATE INDEX IF NOT EXISTS idx_room_amenities_amenity   ON room_amenities (amenity_id);
CREATE INDEX IF NOT EXISTS idx_notifications_user_created ON notifications (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_reviews_room             ON reviews (room_id);
CREATE INDEX IF NOT EXISTS idx_reviews_booking          ON reviews (booking_id);
CREATE INDEX IF NOT EXISTS idx_wishlists_room           ON wishlists (room_id);
