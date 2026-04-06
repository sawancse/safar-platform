-- Allow direct/instant experience bookings without a session
ALTER TABLE listings.experience_bookings ALTER COLUMN session_id DROP NOT NULL;

-- Add requested date for direct bookings (when no session selected)
ALTER TABLE listings.experience_bookings ADD COLUMN IF NOT EXISTS requested_date DATE;
