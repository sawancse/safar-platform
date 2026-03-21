-- Track whether a booking has been reviewed (populated via review.created Kafka event)
ALTER TABLE bookings.bookings ADD COLUMN has_review BOOLEAN DEFAULT FALSE;
ALTER TABLE bookings.bookings ADD COLUMN review_rating INTEGER;
ALTER TABLE bookings.bookings ADD COLUMN reviewed_at TIMESTAMPTZ;
