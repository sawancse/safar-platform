-- V67: Enhance experiences table with richer fields (Airbnb Experiences style)
ALTER TABLE listings.experiences ADD COLUMN IF NOT EXISTS whats_included TEXT;
ALTER TABLE listings.experiences ADD COLUMN IF NOT EXISTS whats_not_included TEXT;
ALTER TABLE listings.experiences ADD COLUMN IF NOT EXISTS itinerary TEXT;
ALTER TABLE listings.experiences ADD COLUMN IF NOT EXISTS meeting_point TEXT;
ALTER TABLE listings.experiences ADD COLUMN IF NOT EXISTS meeting_point_lat NUMERIC(9,6);
ALTER TABLE listings.experiences ADD COLUMN IF NOT EXISTS meeting_point_lng NUMERIC(9,6);
ALTER TABLE listings.experiences ADD COLUMN IF NOT EXISTS accessibility TEXT;
ALTER TABLE listings.experiences ADD COLUMN IF NOT EXISTS cancellation_policy VARCHAR(20) DEFAULT 'FLEXIBLE';
ALTER TABLE listings.experiences ADD COLUMN IF NOT EXISTS min_age INTEGER;
ALTER TABLE listings.experiences ADD COLUMN IF NOT EXISTS is_private BOOLEAN DEFAULT FALSE;
ALTER TABLE listings.experiences ADD COLUMN IF NOT EXISTS group_discount_pct INTEGER;
ALTER TABLE listings.experiences ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ;

-- Index for admin queries
CREATE INDEX IF NOT EXISTS idx_experiences_status ON listings.experiences(status);
CREATE INDEX IF NOT EXISTS idx_experiences_city ON listings.experiences(city);
CREATE INDEX IF NOT EXISTS idx_experiences_host ON listings.experiences(host_id);
