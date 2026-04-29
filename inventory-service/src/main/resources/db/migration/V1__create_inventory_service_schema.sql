CREATE TABLE inventory.inventory_items (
    product_id BIGINT PRIMARY KEY,
    available_quantity INTEGER NOT NULL,
    reserved_quantity INTEGER NOT NULL,
    version BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE inventory.inventory_reservations (
    reservation_code VARCHAR(36) PRIMARY KEY,
    product_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_inventory_reservations_product_id
    ON inventory.inventory_reservations (product_id);

CREATE INDEX idx_inventory_reservations_status
    ON inventory.inventory_reservations (status);

CREATE INDEX idx_inventory_reservations_expires_at
    ON inventory.inventory_reservations (expires_at);
