-- V62: Rental-specific house rules
ALTER TABLE listings.listings ADD COLUMN IF NOT EXISTS rent_payment_day INT DEFAULT 1;
ALTER TABLE listings.listings ADD COLUMN IF NOT EXISTS visitor_policy VARCHAR(20);
ALTER TABLE listings.listings ADD COLUMN IF NOT EXISTS quiet_hours_from VARCHAR(10);
ALTER TABLE listings.listings ADD COLUMN IF NOT EXISTS quiet_hours_until VARCHAR(10);
ALTER TABLE listings.listings ADD COLUMN IF NOT EXISTS smoking_allowed BOOLEAN DEFAULT FALSE;
