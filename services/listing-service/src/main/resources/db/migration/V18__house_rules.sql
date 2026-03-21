-- V18: House rules and service fields for listing wizard
ALTER TABLE listings.listings ADD COLUMN IF NOT EXISTS check_in_from TIME DEFAULT '14:00';
ALTER TABLE listings.listings ADD COLUMN IF NOT EXISTS check_in_until TIME DEFAULT '23:00';
ALTER TABLE listings.listings ADD COLUMN IF NOT EXISTS check_out_from TIME DEFAULT '06:00';
ALTER TABLE listings.listings ADD COLUMN IF NOT EXISTS check_out_until TIME DEFAULT '11:00';
ALTER TABLE listings.listings ADD COLUMN IF NOT EXISTS children_allowed BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE listings.listings ADD COLUMN IF NOT EXISTS parking_type VARCHAR(20) DEFAULT 'NONE';
ALTER TABLE listings.listings ADD COLUMN IF NOT EXISTS breakfast_included BOOLEAN NOT NULL DEFAULT false;
