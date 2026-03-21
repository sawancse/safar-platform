-- Category ratings (Airbnb-style breakdown)
ALTER TABLE reviews.reviews ADD COLUMN rating_cleanliness SMALLINT;
ALTER TABLE reviews.reviews ADD COLUMN rating_location SMALLINT;
ALTER TABLE reviews.reviews ADD COLUMN rating_value SMALLINT;
ALTER TABLE reviews.reviews ADD COLUMN rating_communication SMALLINT;
ALTER TABLE reviews.reviews ADD COLUMN rating_check_in SMALLINT;
ALTER TABLE reviews.reviews ADD COLUMN rating_accuracy SMALLINT;

-- Reply enhancements
ALTER TABLE reviews.reviews ADD COLUMN reply_updated_at TIMESTAMPTZ;

-- Guest name (denormalized for display without cross-service call)
ALTER TABLE reviews.reviews ADD COLUMN guest_name VARCHAR(200);

-- Constraints for category ratings
ALTER TABLE reviews.reviews ADD CONSTRAINT chk_rating_cleanliness CHECK (rating_cleanliness IS NULL OR rating_cleanliness BETWEEN 1 AND 5);
ALTER TABLE reviews.reviews ADD CONSTRAINT chk_rating_location CHECK (rating_location IS NULL OR rating_location BETWEEN 1 AND 5);
ALTER TABLE reviews.reviews ADD CONSTRAINT chk_rating_value CHECK (rating_value IS NULL OR rating_value BETWEEN 1 AND 5);
ALTER TABLE reviews.reviews ADD CONSTRAINT chk_rating_communication CHECK (rating_communication IS NULL OR rating_communication BETWEEN 1 AND 5);
ALTER TABLE reviews.reviews ADD CONSTRAINT chk_rating_check_in CHECK (rating_check_in IS NULL OR rating_check_in BETWEEN 1 AND 5);
ALTER TABLE reviews.reviews ADD CONSTRAINT chk_rating_accuracy CHECK (rating_accuracy IS NULL OR rating_accuracy BETWEEN 1 AND 5);
