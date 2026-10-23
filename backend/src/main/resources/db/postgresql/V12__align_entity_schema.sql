-- ============================================================
-- V12: Align PostgreSQL schema with JPA entity model
-- ============================================================

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'review_photos'
          AND column_name = 'photo_url'
    ) THEN
        ALTER TABLE review_photos RENAME COLUMN photo_url TO photo_filename;
    END IF;
END $$;

ALTER TABLE review_photos ALTER COLUMN photo_filename TYPE VARCHAR(500);
ALTER TABLE rooms ADD COLUMN IF NOT EXISTS slug VARCHAR(255);
ALTER TABLE rooms ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
