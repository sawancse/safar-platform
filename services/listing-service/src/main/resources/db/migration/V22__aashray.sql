-- V22: Aashray (refugee housing) fields
ALTER TABLE listings.listings ADD COLUMN IF NOT EXISTS aashray_ready BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE listings.listings ADD COLUMN IF NOT EXISTS aashray_discount_percent INTEGER DEFAULT 0;
ALTER TABLE listings.listings ADD COLUMN IF NOT EXISTS long_term_monthly_paise BIGINT;
ALTER TABLE listings.listings ADD COLUMN IF NOT EXISTS min_stay_days INTEGER DEFAULT 1;

CREATE INDEX IF NOT EXISTS idx_listings_aashray ON listings.listings (aashray_ready) WHERE aashray_ready = true;
