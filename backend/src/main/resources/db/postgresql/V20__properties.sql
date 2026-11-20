-- =============================================================================
-- V20 — Property (hotel/branch) dimension: single property, multi-property ready
-- -----------------------------------------------------------------------------
-- ADR-0008: today Nhu Villas is one property; data is shaped so a second
-- property needs no schema rewrite. Every villa belongs to a property (default
-- 1 = the existing resort). Vouchers/promotions get an optional property_id
-- (NULL = valid at every property).
-- Additive; existing rows are assigned to property 1 by the column default.
-- Rollback: db/rollback/U20__properties.sql
-- =============================================================================

CREATE TABLE IF NOT EXISTS properties (
    id             BIGSERIAL PRIMARY KEY,
    code           VARCHAR(32)  NOT NULL,
    name           VARCHAR(255) NOT NULL,
    address        VARCHAR(500),
    timezone       VARCHAR(64)  NOT NULL DEFAULT 'Asia/Ho_Chi_Minh',
    currency       VARCHAR(3)   NOT NULL DEFAULT 'VND',
    check_in_time  VARCHAR(5)   NOT NULL DEFAULT '14:00',
    check_out_time VARCHAR(5)   NOT NULL DEFAULT '12:00',
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP,
    CONSTRAINT uq_properties_code UNIQUE (code),
    CONSTRAINT chk_properties_currency CHECK (currency = 'VND')
);

INSERT INTO properties (id, code, name, address)
VALUES (1, 'NHU-DALAT', 'Nhu Villas Đà Lạt', 'Hồ Tuyền Lâm, Đà Lạt, Lâm Đồng')
ON CONFLICT (id) DO NOTHING;
SELECT setval(pg_get_serial_sequence('properties', 'id'), GREATEST((SELECT MAX(id) FROM properties), 1));

ALTER TABLE rooms ADD COLUMN IF NOT EXISTS property_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE rooms DROP CONSTRAINT IF EXISTS fk_rooms_property;
ALTER TABLE rooms ADD CONSTRAINT fk_rooms_property FOREIGN KEY (property_id) REFERENCES properties (id);
CREATE INDEX IF NOT EXISTS idx_rooms_property ON rooms (property_id);

ALTER TABLE vouchers ADD COLUMN IF NOT EXISTS property_id BIGINT;
ALTER TABLE vouchers DROP CONSTRAINT IF EXISTS fk_vouchers_property;
ALTER TABLE vouchers ADD CONSTRAINT fk_vouchers_property FOREIGN KEY (property_id) REFERENCES properties (id);

ALTER TABLE promotions ADD COLUMN IF NOT EXISTS property_id BIGINT;
ALTER TABLE promotions DROP CONSTRAINT IF EXISTS fk_promotions_property;
ALTER TABLE promotions ADD CONSTRAINT fk_promotions_property FOREIGN KEY (property_id) REFERENCES properties (id);
