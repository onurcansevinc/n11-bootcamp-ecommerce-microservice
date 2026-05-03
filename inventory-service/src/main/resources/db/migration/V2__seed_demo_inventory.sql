INSERT INTO inventory.inventory_items (
    product_id,
    available_quantity,
    reserved_quantity,
    version,
    created_at,
    updated_at
)
SELECT
    seed.product_id,
    10 + ((seed.product_id - 1) % 6) * 5,
    0,
    0,
    TIMESTAMP '2026-04-28 20:16:42.888',
    TIMESTAMP '2026-04-28 20:16:42.888'
FROM generate_series(1, 60) AS seed(product_id)
ON CONFLICT (product_id) DO NOTHING;
