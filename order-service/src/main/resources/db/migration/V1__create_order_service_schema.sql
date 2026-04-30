CREATE TABLE IF NOT EXISTS ordering.purchase_orders (
    id VARCHAR(36) PRIMARY KEY,
    customer_id VARCHAR(255) NOT NULL,
    source_cart_id VARCHAR(36) NOT NULL,
    status VARCHAR(50) NOT NULL,
    total_amount NUMERIC(19, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_purchase_orders_customer_id
    ON ordering.purchase_orders (customer_id);

CREATE INDEX IF NOT EXISTS idx_purchase_orders_source_cart_id
    ON ordering.purchase_orders (source_cart_id);

CREATE TABLE IF NOT EXISTS ordering.order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    unit_price NUMERIC(19, 2) NOT NULL,
    quantity INTEGER NOT NULL,
    line_total NUMERIC(19, 2) NOT NULL,
    reservation_code VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id)
        REFERENCES ordering.purchase_orders (id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_order_items_order_id
    ON ordering.order_items (order_id);
