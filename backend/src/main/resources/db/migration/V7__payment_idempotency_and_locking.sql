ALTER TABLE payments ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS payment_refunds (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_id      INT NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    amount          DECIMAL(12, 0) NOT NULL,
    reason          VARCHAR(500),
    status          VARCHAR(32) NOT NULL,
    processed_by    INT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at    TIMESTAMP NULL,
    CONSTRAINT uq_payment_refund_idempotency UNIQUE (payment_id, idempotency_key),
    CONSTRAINT fk_payment_refunds_payment FOREIGN KEY (payment_id) REFERENCES payments(id),
    CONSTRAINT fk_payment_refunds_user FOREIGN KEY (processed_by) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_payment_refunds_payment ON payment_refunds(payment_id);
CREATE INDEX IF NOT EXISTS idx_payment_refunds_status ON payment_refunds(status);
