-- S19: Video review system
CREATE TABLE IF NOT EXISTS bookings.video_reviews (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id          UUID NOT NULL REFERENCES bookings.bookings(id),
    guest_id            UUID NOT NULL,
    listing_id          UUID NOT NULL,
    s3_key              TEXT NOT NULL,
    cdn_url             TEXT,
    duration_seconds    INTEGER,
    moderation_status   VARCHAR(20) DEFAULT 'PENDING'
                            CHECK (moderation_status IN ('PENDING','APPROVED','REJECTED')),
    moderation_reason   TEXT,
    created_at          TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_video_reviews_listing ON bookings.video_reviews(listing_id);
CREATE INDEX IF NOT EXISTS idx_video_reviews_guest   ON bookings.video_reviews(guest_id);

-- S20: Track wallet credits applied to bookings
ALTER TABLE bookings.bookings
    ADD COLUMN IF NOT EXISTS wallet_credits_applied_paise BIGINT NOT NULL DEFAULT 0;
