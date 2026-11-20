-- Manual rollback for V15 (psql -v ON_ERROR_STOP=1 -f). Data-preserving.
BEGIN;
ALTER TABLE rooms DROP CONSTRAINT IF EXISTS uq_rooms_slug;
ALTER TABLE rooms ALTER COLUMN version DROP NOT NULL;
DROP INDEX IF EXISTS idx_room_gallery_images_room;
DROP INDEX IF EXISTS idx_review_photos_review;
DROP INDEX IF EXISTS idx_promotion_rooms_room;
DROP INDEX IF EXISTS idx_room_amenities_amenity;
DROP INDEX IF EXISTS idx_notifications_user_created;
DROP INDEX IF EXISTS idx_reviews_room;
DROP INDEX IF EXISTS idx_reviews_booking;
DROP INDEX IF EXISTS idx_wishlists_room;
DELETE FROM flyway_schema_history WHERE version = '15';
COMMIT;