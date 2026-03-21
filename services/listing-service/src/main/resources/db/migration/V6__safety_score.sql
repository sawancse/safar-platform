ALTER TABLE listings.listings ADD COLUMN IF NOT EXISTS safety_score NUMERIC(4,1);
ALTER TABLE listings.listings ADD COLUMN IF NOT EXISTS safety_label VARCHAR(20);
ALTER TABLE listings.listings ADD COLUMN IF NOT EXISTS women_friendly BOOLEAN DEFAULT false;
ALTER TABLE listings.listings ADD COLUMN IF NOT EXISTS safety_updated_at TIMESTAMPTZ;
