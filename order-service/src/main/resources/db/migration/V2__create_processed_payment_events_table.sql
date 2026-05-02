CREATE TABLE IF NOT EXISTS ordering.processed_payment_events (
    event_id VARCHAR(36) PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    order_id VARCHAR(36) NOT NULL,
    processed_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_processed_payment_events_order_id
    ON ordering.processed_payment_events (order_id);
