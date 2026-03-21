-- Listing Archive & Suspension support
ALTER TABLE listings.listings ADD COLUMN IF NOT EXISTS archive_reason VARCHAR(30);
ALTER TABLE listings.listings ADD COLUMN IF NOT EXISTS archive_note TEXT;
ALTER TABLE listings.listings ADD COLUMN IF NOT EXISTS archived_by UUID;
ALTER TABLE listings.listings ADD COLUMN IF NOT EXISTS archived_at TIMESTAMP;
ALTER TABLE listings.listings ADD COLUMN IF NOT EXISTS previous_status VARCHAR(30);

CREATE INDEX IF NOT EXISTS idx_listings_archive_reason ON listings.listings(archive_reason);
