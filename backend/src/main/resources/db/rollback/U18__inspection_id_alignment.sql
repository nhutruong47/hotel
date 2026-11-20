-- Manual rollback for V18. Narrowing fails (by design) if an id exceeds INT range.
BEGIN;
ALTER TABLE villa_inspections ALTER COLUMN inspector_id TYPE INT;
ALTER TABLE villa_inspections ALTER COLUMN room_id TYPE INT;
ALTER TABLE villa_inspections ALTER COLUMN id TYPE INT;
ALTER SEQUENCE IF EXISTS villa_inspections_id_seq AS INT;
DELETE FROM flyway_schema_history WHERE version = '18';
COMMIT;