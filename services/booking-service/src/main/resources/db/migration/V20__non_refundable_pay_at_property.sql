-- Feature 1: Non-refundable bookings
ALTER TABLE bookings.bookings ADD COLUMN IF NOT EXISTS non_refundable BOOLEAN DEFAULT FALSE;
ALTER TABLE bookings.bookings ADD COLUMN IF NOT EXISTS non_refundable_discount_paise BIGINT DEFAULT 0;

-- Feature 2: Pay at Property
ALTER TABLE bookings.bookings ADD COLUMN IF NOT EXISTS payment_mode VARCHAR(20) DEFAULT 'PREPAID';
ALTER TABLE bookings.bookings ADD COLUMN IF NOT EXISTS prepaid_amount_paise BIGINT;
ALTER TABLE bookings.bookings ADD COLUMN IF NOT EXISTS due_at_property_paise BIGINT;
