ALTER TABLE bookings.bookings ADD COLUMN IF NOT EXISTS group_booking_id UUID;
ALTER TABLE bookings.bookings ADD COLUMN IF NOT EXISTS is_primary_booking BOOLEAN DEFAULT true;
