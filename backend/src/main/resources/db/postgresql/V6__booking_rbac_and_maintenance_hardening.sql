-- Canonicalise legacy booking states before the Java enum is narrowed.
UPDATE users SET role = UPPER(role);
UPDATE users SET role = 'USER' WHERE role NOT IN ('USER', 'STAFF', 'MANAGER', 'ADMIN');

UPDATE bookings SET status = 'PENDING_PAYMENT' WHERE status = 'AWAITING_APPROVAL';
UPDATE bookings SET status = 'PAID' WHERE status = 'CONFIRMED';
UPDATE bookings SET status = 'CANCELLED' WHERE status = 'REJECTED';

UPDATE booking_status_transitions SET from_status = 'PENDING_PAYMENT' WHERE from_status = 'AWAITING_APPROVAL';
UPDATE booking_status_transitions SET to_status = 'PENDING_PAYMENT' WHERE to_status = 'AWAITING_APPROVAL';
UPDATE booking_status_transitions SET from_status = 'PAID' WHERE from_status = 'CONFIRMED';
UPDATE booking_status_transitions SET to_status = 'PAID' WHERE to_status = 'CONFIRMED';
UPDATE booking_status_transitions SET from_status = 'CANCELLED' WHERE from_status = 'REJECTED';
UPDATE booking_status_transitions SET to_status = 'CANCELLED' WHERE to_status = 'REJECTED';

-- hold_expires_at is now the authoritative payment/inventory expiry.
UPDATE bookings
SET hold_expires_at = COALESCE(hold_expires_at, payment_deadline),
    payment_deadline = COALESCE(hold_expires_at, payment_deadline)
WHERE hold_expires_at IS NULL OR payment_deadline IS DISTINCT FROM hold_expires_at;

ALTER TABLE bookings ADD COLUMN IF NOT EXISTS refund_bank_name VARCHAR(255);
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS refund_account_number VARCHAR(255);
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS refund_account_name VARCHAR(255);

CREATE TABLE IF NOT EXISTS villa_maintenances (
    id          BIGSERIAL PRIMARY KEY,
    room_id     BIGINT       NOT NULL,
    start_date  DATE         NOT NULL,
    end_date    DATE         NOT NULL,
    reason      VARCHAR(500),
    status      VARCHAR(32)  NOT NULL DEFAULT 'SCHEDULED',
    created_by  BIGINT,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_maint_room FOREIGN KEY (room_id) REFERENCES rooms(id),
    CONSTRAINT fk_maint_user FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT chk_maint_dates CHECK (end_date > start_date)
);

CREATE INDEX IF NOT EXISTS idx_maint_room_dates ON villa_maintenances(room_id, start_date, end_date);
CREATE INDEX IF NOT EXISTS idx_maint_status ON villa_maintenances(status);

ALTER TABLE users DROP CONSTRAINT IF EXISTS chk_users_role;
ALTER TABLE users ADD CONSTRAINT chk_users_role
    CHECK (role IN ('USER', 'STAFF', 'MANAGER', 'ADMIN'));

ALTER TABLE bookings DROP CONSTRAINT IF EXISTS chk_bookings_status;
ALTER TABLE bookings ADD CONSTRAINT chk_bookings_status
    CHECK (status IN ('PENDING_PAYMENT', 'PAID', 'CHECKED_IN', 'CHECKED_OUT',
                      'COMPLETED', 'CANCELLED', 'EXPIRED', 'NO_SHOW'));
