-- V20: Commercial listing fields
ALTER TABLE listings.listings ADD COLUMN IF NOT EXISTS area_sqft INTEGER;
ALTER TABLE listings.listings ADD COLUMN IF NOT EXISTS operating_hours_from TIME;
ALTER TABLE listings.listings ADD COLUMN IF NOT EXISTS operating_hours_until TIME;
