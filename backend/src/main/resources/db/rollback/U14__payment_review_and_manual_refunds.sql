-- =============================================================================
-- Manual rollback for V14__payment_review_and_manual_refunds.sql (run with psql).
-- Refuses to run while manual refunds are pending, because the old status CHECK
-- cannot hold them — confirm or fail them first.
-- =============================================================================
BEGIN;
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM payment_refunds WHERE status = 'PENDING_MANUAL') THEN
        RAISE EXCEPTION 'U14 aborted: PENDING_MANUAL refunds exist; resolve them before rolling back';
    END IF;
END $$;
ALTER TABLE payment_refunds DROP CONSTRAINT IF EXISTS chk_payment_refund_manual_reference;
ALTER TABLE payment_refunds DROP CONSTRAINT IF EXISTS fk_payment_refunds_confirmed_by;
ALTER TABLE payment_refunds DROP COLUMN IF EXISTS confirmed_at;
ALTER TABLE payment_refunds DROP COLUMN IF EXISTS confirmed_by;
ALTER TABLE payment_refunds DROP COLUMN IF EXISTS bank_reference;
ALTER TABLE payment_refunds DROP CONSTRAINT IF EXISTS chk_payment_refund_status;
ALTER TABLE payment_refunds ADD CONSTRAINT chk_payment_refund_status
    CHECK (status IN ('PROCESSING', 'SUCCEEDED', 'FAILED'));
DROP INDEX IF EXISTS idx_payments_review_required;
ALTER TABLE payments DROP COLUMN IF EXISTS review_reason;
ALTER TABLE payments DROP COLUMN IF EXISTS review_required;
DELETE FROM flyway_schema_history WHERE version = '14';
COMMIT;
