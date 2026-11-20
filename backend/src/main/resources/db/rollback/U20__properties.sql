-- Manual rollback for V20. Refuses while more than one property exists.
BEGIN;
DO $$
BEGIN
    IF (SELECT count(*) FROM properties) > 1 THEN
        RAISE EXCEPTION 'U20 aborted: more than one property exists; migrate rooms back to a single property first';
    END IF;
END $$;
ALTER TABLE promotions DROP CONSTRAINT IF EXISTS fk_promotions_property, DROP COLUMN IF EXISTS property_id;
ALTER TABLE vouchers   DROP CONSTRAINT IF EXISTS fk_vouchers_property, DROP COLUMN IF EXISTS property_id;
DROP INDEX IF EXISTS idx_rooms_property;
ALTER TABLE rooms      DROP CONSTRAINT IF EXISTS fk_rooms_property, DROP COLUMN IF EXISTS property_id;
DROP TABLE IF EXISTS properties;
DELETE FROM flyway_schema_history WHERE version = '20';
COMMIT;