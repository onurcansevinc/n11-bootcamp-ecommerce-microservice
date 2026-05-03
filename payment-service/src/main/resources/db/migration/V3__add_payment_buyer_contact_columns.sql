ALTER TABLE payments.payments
    ADD COLUMN IF NOT EXISTS buyer_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS buyer_surname VARCHAR(255),
    ADD COLUMN IF NOT EXISTS buyer_email VARCHAR(255),
    ADD COLUMN IF NOT EXISTS buyer_gsm_number VARCHAR(50);

UPDATE payments.payments
SET buyer_name = COALESCE(buyer_name, 'Unknown'),
    buyer_surname = COALESCE(buyer_surname, 'Customer'),
    buyer_email = COALESCE(buyer_email, 'unknown@example.com'),
    buyer_gsm_number = COALESCE(buyer_gsm_number, '0000000000');

ALTER TABLE payments.payments
    ALTER COLUMN buyer_name SET NOT NULL,
    ALTER COLUMN buyer_surname SET NOT NULL,
    ALTER COLUMN buyer_email SET NOT NULL,
    ALTER COLUMN buyer_gsm_number SET NOT NULL;
