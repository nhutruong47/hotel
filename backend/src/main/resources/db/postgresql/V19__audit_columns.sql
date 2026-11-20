-- =============================================================================
-- V19 — Standard audit columns for staff-managed catalogue/operations tables
-- -----------------------------------------------------------------------------
-- Who created / last changed a villa, price, voucher, promotion, maintenance
-- window, inspection, blog post or FAQ was not recorded anywhere. Adds
-- created_at / updated_at / created_by / updated_by where missing; the
-- application fills them via AuditStampListener (common/audit). created_by and
-- updated_by reference users and survive user deletion (ON DELETE SET NULL).
-- villa_maintenances already has created_by (V6) — only updated_* are added.
-- Additive only. Rollback: db/rollback/U19__audit_columns.sql
-- =============================================================================

-- rooms, room_types, amenities, vouchers: full set
ALTER TABLE rooms      ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE rooms      ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;
ALTER TABLE rooms      ADD COLUMN IF NOT EXISTS created_by BIGINT REFERENCES users (id) ON DELETE SET NULL;
ALTER TABLE rooms      ADD COLUMN IF NOT EXISTS updated_by BIGINT REFERENCES users (id) ON DELETE SET NULL;

ALTER TABLE room_types ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE room_types ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;
ALTER TABLE room_types ADD COLUMN IF NOT EXISTS created_by BIGINT REFERENCES users (id) ON DELETE SET NULL;
ALTER TABLE room_types ADD COLUMN IF NOT EXISTS updated_by BIGINT REFERENCES users (id) ON DELETE SET NULL;

ALTER TABLE amenities  ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE amenities  ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;
ALTER TABLE amenities  ADD COLUMN IF NOT EXISTS created_by BIGINT REFERENCES users (id) ON DELETE SET NULL;
ALTER TABLE amenities  ADD COLUMN IF NOT EXISTS updated_by BIGINT REFERENCES users (id) ON DELETE SET NULL;

ALTER TABLE vouchers   ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE vouchers   ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;
ALTER TABLE vouchers   ADD COLUMN IF NOT EXISTS created_by BIGINT REFERENCES users (id) ON DELETE SET NULL;
ALTER TABLE vouchers   ADD COLUMN IF NOT EXISTS updated_by BIGINT REFERENCES users (id) ON DELETE SET NULL;

-- promotions, faq_items, villa_inspections: timestamps exist, add actors
ALTER TABLE promotions        ADD COLUMN IF NOT EXISTS created_by BIGINT REFERENCES users (id) ON DELETE SET NULL;
ALTER TABLE promotions        ADD COLUMN IF NOT EXISTS updated_by BIGINT REFERENCES users (id) ON DELETE SET NULL;
ALTER TABLE faq_items         ADD COLUMN IF NOT EXISTS created_by BIGINT REFERENCES users (id) ON DELETE SET NULL;
ALTER TABLE faq_items         ADD COLUMN IF NOT EXISTS updated_by BIGINT REFERENCES users (id) ON DELETE SET NULL;
ALTER TABLE villa_inspections ADD COLUMN IF NOT EXISTS created_by BIGINT REFERENCES users (id) ON DELETE SET NULL;
ALTER TABLE villa_inspections ADD COLUMN IF NOT EXISTS updated_by BIGINT REFERENCES users (id) ON DELETE SET NULL;

-- blogs: created_at exists
ALTER TABLE blogs ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;
ALTER TABLE blogs ADD COLUMN IF NOT EXISTS created_by BIGINT REFERENCES users (id) ON DELETE SET NULL;
ALTER TABLE blogs ADD COLUMN IF NOT EXISTS updated_by BIGINT REFERENCES users (id) ON DELETE SET NULL;

-- villa_maintenances: created_at / created_by exist (V6)
ALTER TABLE villa_maintenances ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;
ALTER TABLE villa_maintenances ADD COLUMN IF NOT EXISTS updated_by BIGINT REFERENCES users (id) ON DELETE SET NULL;
