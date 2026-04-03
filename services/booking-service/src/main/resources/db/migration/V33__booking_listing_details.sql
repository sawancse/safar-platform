-- Add listing details to booking for richer trip cards
ALTER TABLE bookings.bookings ADD COLUMN listing_city VARCHAR(100);
ALTER TABLE bookings.bookings ADD COLUMN listing_type VARCHAR(30);
ALTER TABLE bookings.bookings ADD COLUMN listing_photo_url TEXT;
ALTER TABLE bookings.bookings ADD COLUMN host_name VARCHAR(255);
ALTER TABLE bookings.bookings ADD COLUMN listing_address TEXT;
