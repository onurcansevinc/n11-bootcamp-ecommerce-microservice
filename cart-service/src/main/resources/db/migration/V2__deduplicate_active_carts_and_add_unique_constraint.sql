CREATE TEMP TABLE cart_active_cart_dedup AS
SELECT id AS duplicate_id,
       survivor_id
FROM (
    SELECT id,
           customer_id,
           status,
           FIRST_VALUE(id) OVER (
               PARTITION BY customer_id, status
               ORDER BY updated_at DESC, created_at DESC, id DESC
           ) AS survivor_id,
           ROW_NUMBER() OVER (
               PARTITION BY customer_id, status
               ORDER BY updated_at DESC, created_at DESC, id DESC
           ) AS row_num
    FROM cart.carts
) ranked
WHERE row_num > 1;

UPDATE cart.cart_items survivor_item
SET quantity = survivor_item.quantity + duplicate_item.quantity,
    updated_at = GREATEST(survivor_item.updated_at, duplicate_item.updated_at)
FROM cart_active_cart_dedup dedup
JOIN cart.cart_items duplicate_item
    ON duplicate_item.cart_id = dedup.duplicate_id
WHERE survivor_item.cart_id = dedup.survivor_id
  AND survivor_item.product_id = duplicate_item.product_id;

UPDATE cart.cart_items moving_item
SET cart_id = dedup.survivor_id
FROM cart_active_cart_dedup dedup
WHERE moving_item.cart_id = dedup.duplicate_id
  AND NOT EXISTS (
      SELECT 1
      FROM cart.cart_items survivor_item
      WHERE survivor_item.cart_id = dedup.survivor_id
        AND survivor_item.product_id = moving_item.product_id
  );

UPDATE cart.carts survivor_cart
SET updated_at = GREATEST(survivor_cart.updated_at, dedup_summary.max_duplicate_updated_at)
FROM (
    SELECT dedup.survivor_id,
           MAX(duplicate_cart.updated_at) AS max_duplicate_updated_at
    FROM cart_active_cart_dedup dedup
    JOIN cart.carts duplicate_cart
      ON duplicate_cart.id = dedup.duplicate_id
    GROUP BY dedup.survivor_id
) dedup_summary
WHERE survivor_cart.id = dedup_summary.survivor_id;

DELETE FROM cart.cart_items duplicate_item
USING cart_active_cart_dedup dedup
WHERE duplicate_item.cart_id = dedup.duplicate_id;

DELETE FROM cart.carts duplicate_cart
USING cart_active_cart_dedup dedup
WHERE duplicate_cart.id = dedup.duplicate_id;

DROP TABLE cart_active_cart_dedup;

ALTER TABLE cart.carts
    ADD CONSTRAINT uk_carts_customer_status UNIQUE (customer_id, status);
