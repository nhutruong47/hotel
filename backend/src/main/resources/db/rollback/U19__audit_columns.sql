-- Manual rollback for V19. Drops the audit columns added by V19 (their values are lost).
BEGIN;
ALTER TABLE rooms      DROP COLUMN IF EXISTS updated_by, DROP COLUMN IF EXISTS created_by, DROP COLUMN IF EXISTS updated_at, DROP COLUMN IF EXISTS created_at;
ALTER TABLE room_types DROP COLUMN IF EXISTS updated_by, DROP COLUMN IF EXISTS created_by, DROP COLUMN IF EXISTS updated_at, DROP COLUMN IF EXISTS created_at;
ALTER TABLE amenities  DROP COLUMN IF EXISTS updated_by, DROP COLUMN IF EXISTS created_by, DROP COLUMN IF EXISTS updated_at, DROP COLUMN IF EXISTS created_at;
ALTER TABLE vouchers   DROP COLUMN IF EXISTS updated_by, DROP COLUMN IF EXISTS created_by, DROP COLUMN IF EXISTS updated_at, DROP COLUMN IF EXISTS created_at;
ALTER TABLE promotions DROP COLUMN IF EXISTS updated_by, DROP COLUMN IF EXISTS created_by;
ALTER TABLE faq_items  DROP COLUMN IF EXISTS updated_by, DROP COLUMN IF EXISTS created_by;
ALTER TABLE villa_inspections DROP COLUMN IF EXISTS updated_by, DROP COLUMN IF EXISTS created_by;
ALTER TABLE blogs      DROP COLUMN IF EXISTS updated_by, DROP COLUMN IF EXISTS created_by, DROP COLUMN IF EXISTS updated_at;
ALTER TABLE villa_maintenances DROP COLUMN IF EXISTS updated_by, DROP COLUMN IF EXISTS updated_at;
DELETE FROM flyway_schema_history WHERE version = '19';
COMMIT;