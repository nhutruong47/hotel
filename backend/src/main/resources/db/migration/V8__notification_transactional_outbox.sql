CREATE TABLE IF NOT EXISTS notification_outbox (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type   VARCHAR(64) NOT NULL,
    routing_key  VARCHAR(128) NOT NULL,
    payload      TEXT,
    status       VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    attempts     INT NOT NULL DEFAULT 0,
    available_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP NULL,
    last_error   VARCHAR(1000),
    version      BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_notification_outbox_ready
    ON notification_outbox(status, available_at, created_at);
