CREATE TABLE listings.ical_feeds (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id UUID NOT NULL REFERENCES listings.listings(id) ON DELETE CASCADE,
    feed_url TEXT NOT NULL,
    feed_name VARCHAR(100),
    last_synced_at TIMESTAMPTZ,
    sync_interval_hours INTEGER DEFAULT 6,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX idx_ical_listing ON listings.ical_feeds(listing_id);
