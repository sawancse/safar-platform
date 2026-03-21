-- Extended stay and Aashray booking fields
ALTER TABLE bookings.bookings ADD COLUMN IF NOT EXISTS booking_type VARCHAR(20) NOT NULL DEFAULT 'SHORT_TERM';
ALTER TABLE bookings.bookings ADD COLUMN IF NOT EXISTS organization_id UUID;
ALTER TABLE bookings.bookings ADD COLUMN IF NOT EXISTS case_worker_id UUID;
ALTER TABLE bookings.bookings ADD COLUMN IF NOT EXISTS monthly_rate_paise BIGINT;
