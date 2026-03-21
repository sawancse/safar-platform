-- V17: Add fields for comprehensive search filters
ALTER TABLE listings.listings ADD COLUMN IF NOT EXISTS star_rating INTEGER;
ALTER TABLE listings.listings ADD COLUMN IF NOT EXISTS cancellation_policy VARCHAR(20) DEFAULT 'MODERATE';
ALTER TABLE listings.listings ADD COLUMN IF NOT EXISTS meal_plan VARCHAR(30) DEFAULT 'NONE';
ALTER TABLE listings.listings ADD COLUMN IF NOT EXISTS bed_types text[];
ALTER TABLE listings.listings ADD COLUMN IF NOT EXISTS accessibility_features text[];
ALTER TABLE listings.listings ADD COLUMN IF NOT EXISTS free_cancellation BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE listings.listings ADD COLUMN IF NOT EXISTS no_prepayment BOOLEAN NOT NULL DEFAULT false;

CREATE INDEX IF NOT EXISTS idx_listings_star_rating ON listings.listings (star_rating);
CREATE INDEX IF NOT EXISTS idx_listings_cancellation_policy ON listings.listings (cancellation_policy);
CREATE INDEX IF NOT EXISTS idx_listings_meal_plan ON listings.listings (meal_plan);
