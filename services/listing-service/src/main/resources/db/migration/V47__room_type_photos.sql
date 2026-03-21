-- Room type photos (Booking.com style — per room type images)
ALTER TABLE listings.room_types ADD COLUMN IF NOT EXISTS primary_photo_url VARCHAR(500);
ALTER TABLE listings.room_types ADD COLUMN IF NOT EXISTS photo_urls TEXT;
