CREATE TABLE IF NOT EXISTS payments.payments (
    id VARCHAR(36) PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL,
    customer_id VARCHAR(255) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    external_payment_id VARCHAR(255) NOT NULL,
    checkout_url VARCHAR(500) NOT NULL,
    failure_reason TEXT,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_payments_order_id
    ON payments.payments (order_id);

CREATE INDEX IF NOT EXISTS idx_payments_customer_id
    ON payments.payments (customer_id);
