-- Feature 1: Non-refundable rate plan
ALTER TABLE listings.listings ADD COLUMN IF NOT EXISTS non_refundable_discount_percent INT;

-- Feature 2: Pay at Property
ALTER TABLE listings.listings ADD COLUMN IF NOT EXISTS payment_mode VARCHAR(20) DEFAULT 'PREPAID';
ALTER TABLE listings.listings ADD COLUMN IF NOT EXISTS pay_at_property_enabled BOOLEAN DEFAULT FALSE;
ALTER TABLE listings.listings ADD COLUMN IF NOT EXISTS partial_prepaid_percent INT;

-- Feature 3: Discovery categories (India vibe-based)
ALTER TABLE listings.listings ADD COLUMN IF NOT EXISTS discovery_categories VARCHAR(500);
CREATE INDEX IF NOT EXISTS idx_listings_discovery ON listings.listings(discovery_categories);
CREATE INDEX IF NOT EXISTS idx_listings_payment_mode ON listings.listings(payment_mode);
