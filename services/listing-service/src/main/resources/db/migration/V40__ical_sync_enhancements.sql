-- Add source tracking to availability
ALTER TABLE listings.availability ADD COLUMN IF NOT EXISTS source VARCHAR(50);
ALTER TABLE listings.availability ADD COLUMN IF NOT EXISTS external_booking_ref VARCHAR(255);

-- Enhance ical_feeds with sync metadata
ALTER TABLE listings.ical_feeds ADD COLUMN IF NOT EXISTS source_platform VARCHAR(50);
ALTER TABLE listings.ical_feeds ADD COLUMN IF NOT EXISTS last_sync_status VARCHAR(20);
ALTER TABLE listings.ical_feeds ADD COLUMN IF NOT EXISTS last_error_message TEXT;
ALTER TABLE listings.ical_feeds ADD COLUMN IF NOT EXISTS sync_failure_count INTEGER DEFAULT 0;
ALTER TABLE listings.ical_feeds ADD COLUMN IF NOT EXISTS etag VARCHAR(255);
ALTER TABLE listings.ical_feeds ADD COLUMN IF NOT EXISTS last_modified_header VARCHAR(255);
ALTER TABLE listings.ical_feeds ADD COLUMN IF NOT EXISTS room_type_id UUID;

-- Index for source-aware queries
CREATE INDEX IF NOT EXISTS idx_availability_source ON listings.availability(listing_id, source);
