-- =============================================================================
-- V14 — Payment reconciliation flag + manual (bank transfer) refund workflow
-- -----------------------------------------------------------------------------
-- ADR-0009 Q4: money that arrives late, twice or with the wrong amount is always
--   recorded as a payment and flagged for reconciliation instead of being
--   rejected/lost (Stripe webhook used to roll back and retry forever; SePay
--   mismatches were not stored at all).
-- ADR-0009 Q5: refunds of non-card payments are a two-step manual workflow:
--   PENDING_MANUAL (requested) -> SUCCEEDED (ADMIN confirmed the bank transfer
--   with its reference) or FAILED. Previously the gateway adapter reported a
--   fake success for these payments.
-- Additive only: no existing row is modified (new columns get defaults/NULL).
-- Rollback: db/rollback/U14__payment_review_and_manual_refunds.sql
-- =============================================================================

ALTER TABLE payments ADD COLUMN IF NOT EXISTS review_required BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE payments ADD COLUMN IF NOT EXISTS review_reason VARCHAR(500);
CREATE INDEX IF NOT EXISTS idx_payments_review_required ON payments (created_at DESC) WHERE review_required;

ALTER TABLE payment_refunds DROP CONSTRAINT IF EXISTS chk_payment_refund_status;
ALTER TABLE payment_refunds ADD CONSTRAINT chk_payment_refund_status
    CHECK (status IN ('PROCESSING', 'PENDING_MANUAL', 'SUCCEEDED', 'FAILED'));

ALTER TABLE payment_refunds ADD COLUMN IF NOT EXISTS bank_reference VARCHAR(128);
ALTER TABLE payment_refunds ADD COLUMN IF NOT EXISTS confirmed_by BIGINT;
ALTER TABLE payment_refunds ADD COLUMN IF NOT EXISTS confirmed_at TIMESTAMP;
ALTER TABLE payment_refunds DROP CONSTRAINT IF EXISTS fk_payment_refunds_confirmed_by;
ALTER TABLE payment_refunds ADD CONSTRAINT fk_payment_refunds_confirmed_by
    FOREIGN KEY (confirmed_by) REFERENCES users (id);

-- A confirmed manual refund must carry the bank transfer reference.
ALTER TABLE payment_refunds DROP CONSTRAINT IF EXISTS chk_payment_refund_manual_reference;
ALTER TABLE payment_refunds ADD CONSTRAINT chk_payment_refund_manual_reference
    CHECK (confirmed_at IS NULL OR bank_reference IS NOT NULL);
