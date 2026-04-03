-- Add payment tracking fields to chef_bookings
ALTER TABLE chefs.chef_bookings
    ADD COLUMN advance_amount_paise    BIGINT DEFAULT 0,
    ADD COLUMN balance_amount_paise    BIGINT DEFAULT 0,
    ADD COLUMN razorpay_order_id       VARCHAR(100),
    ADD COLUMN razorpay_payment_id     VARCHAR(100),
    ADD COLUMN payment_status          VARCHAR(20) DEFAULT 'UNPAID';

CREATE INDEX idx_cb_payment_status ON chefs.chef_bookings(payment_status);

-- Backfill existing bookings: treat as fully unpaid
UPDATE chefs.chef_bookings
   SET advance_amount_paise = 0,
       balance_amount_paise = total_amount_paise,
       payment_status = 'UNPAID'
 WHERE advance_amount_paise IS NULL;
