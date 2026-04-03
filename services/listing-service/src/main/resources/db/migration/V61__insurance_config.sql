-- V61: Configurable micro-insurance per listing
ALTER TABLE listings.listings ADD COLUMN IF NOT EXISTS insurance_enabled BOOLEAN DEFAULT FALSE;
ALTER TABLE listings.listings ADD COLUMN IF NOT EXISTS insurance_amount_paise BIGINT;
ALTER TABLE listings.listings ADD COLUMN IF NOT EXISTS insurance_type VARCHAR(30);
