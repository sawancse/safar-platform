-- Feature 5: Double-blind reviews (Airbnb model)
-- Both host and guest submit reviews independently; both are revealed simultaneously

-- Host-to-guest review
ALTER TABLE reviews.reviews ADD COLUMN IF NOT EXISTS host_rating SMALLINT;
ALTER TABLE reviews.reviews ADD COLUMN IF NOT EXISTS host_comment TEXT;
ALTER TABLE reviews.reviews ADD COLUMN IF NOT EXISTS host_reviewed_at TIMESTAMP;

-- Visibility control
ALTER TABLE reviews.reviews ADD COLUMN IF NOT EXISTS guest_review_visible BOOLEAN DEFAULT FALSE;
ALTER TABLE reviews.reviews ADD COLUMN IF NOT EXISTS host_review_visible BOOLEAN DEFAULT FALSE;
ALTER TABLE reviews.reviews ADD COLUMN IF NOT EXISTS both_revealed_at TIMESTAMP;

-- Review window tracking
ALTER TABLE reviews.reviews ADD COLUMN IF NOT EXISTS review_deadline TIMESTAMP; -- 14 days after checkout
