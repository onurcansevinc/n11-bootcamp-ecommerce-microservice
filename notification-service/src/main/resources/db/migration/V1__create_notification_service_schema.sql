CREATE TABLE IF NOT EXISTS notifications.notifications (
    id VARCHAR(36) PRIMARY KEY,
    event_id VARCHAR(36) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    order_id VARCHAR(36) NOT NULL,
    customer_id VARCHAR(255) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    provider VARCHAR(20) NOT NULL,
    recipient VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    content VARCHAR(4000) NOT NULL,
    status VARCHAR(20) NOT NULL,
    failure_reason VARCHAR(1000),
    sent_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_notifications_order_id
    ON notifications.notifications (order_id);

CREATE INDEX IF NOT EXISTS idx_notifications_event_id
    ON notifications.notifications (event_id);

CREATE TABLE IF NOT EXISTS notifications.processed_notification_events (
    event_id VARCHAR(36) PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    order_id VARCHAR(36) NOT NULL,
    processed_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_processed_notification_events_order_id
    ON notifications.processed_notification_events (order_id);
