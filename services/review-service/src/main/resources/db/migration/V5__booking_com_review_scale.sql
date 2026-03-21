-- Upgrade review rating scale from 1-5 to 1-10 (Booking.com style)
-- Add new Booking.com category ratings: Staff, Facilities, Comfort, Free WiFi

ALTER TABLE reviews.reviews ADD COLUMN IF NOT EXISTS rating_staff SMALLINT;
ALTER TABLE reviews.reviews ADD COLUMN IF NOT EXISTS rating_facilities SMALLINT;
ALTER TABLE reviews.reviews ADD COLUMN IF NOT EXISTS rating_comfort SMALLINT;
ALTER TABLE reviews.reviews ADD COLUMN IF NOT EXISTS rating_free_wifi SMALLINT;
