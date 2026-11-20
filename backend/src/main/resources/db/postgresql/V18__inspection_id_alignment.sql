-- =============================================================================
-- V18 — villa_inspections key types aligned with the rest of the schema
-- -----------------------------------------------------------------------------
-- V11 created villa_inspections with SERIAL/INT keys referencing BIGINT columns
-- (rooms.id, users.id). Widen to BIGINT like every other table. Widening is
-- lossless; the table is small, so the rewrite is cheap.
-- Timestamps stay TIMESTAMPTZ here (ADR-0010: no system-wide timestamp change
-- without a concrete need).
-- Rollback: db/rollback/U18__inspection_id_alignment.sql
-- =============================================================================

ALTER TABLE villa_inspections ALTER COLUMN id TYPE BIGINT;
ALTER SEQUENCE IF EXISTS villa_inspections_id_seq AS BIGINT;
ALTER TABLE villa_inspections ALTER COLUMN room_id TYPE BIGINT;
ALTER TABLE villa_inspections ALTER COLUMN inspector_id TYPE BIGINT;
