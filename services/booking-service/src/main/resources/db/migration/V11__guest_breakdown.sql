ALTER TABLE bookings.bookings ADD COLUMN IF NOT EXISTS adults_count INTEGER;
ALTER TABLE bookings.bookings ADD COLUMN IF NOT EXISTS children_count INTEGER DEFAULT 0;
ALTER TABLE bookings.bookings ADD COLUMN IF NOT EXISTS infants_count INTEGER DEFAULT 0;
ALTER TABLE bookings.bookings ADD COLUMN IF NOT EXISTS pets_count INTEGER DEFAULT 0;

-- Backfill: existing bookings treat guestsCount as adultsCount
UPDATE bookings.bookings SET adults_count = guests_count WHERE adults_count IS NULL;
