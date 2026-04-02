-- V8: Support experience reviews alongside listing reviews
ALTER TABLE reviews.reviews ADD COLUMN IF NOT EXISTS target_type VARCHAR(20) DEFAULT 'LISTING';
ALTER TABLE reviews.reviews ADD COLUMN IF NOT EXISTS experience_id UUID;
ALTER TABLE reviews.reviews ALTER COLUMN listing_id DROP NOT NULL;

CREATE INDEX IF NOT EXISTS idx_reviews_experience ON reviews.reviews(experience_id);
CREATE INDEX IF NOT EXISTS idx_reviews_target_type ON reviews.reviews(target_type);
