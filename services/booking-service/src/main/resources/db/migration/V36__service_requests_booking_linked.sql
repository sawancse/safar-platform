-- V36: Extend maintenance_requests to support booking-linked service requests (hotel/apartment)
-- Make tenancy_id nullable, add booking_id and property_type columns

ALTER TABLE bookings.maintenance_requests
    ALTER COLUMN tenancy_id DROP NOT NULL;

ALTER TABLE bookings.maintenance_requests
    ADD COLUMN IF NOT EXISTS booking_id UUID,
    ADD COLUMN IF NOT EXISTS property_type VARCHAR(20),
    ADD COLUMN IF NOT EXISTS guest_id UUID;

-- Index for booking-based lookups
CREATE INDEX IF NOT EXISTS idx_maintenance_booking_id
    ON bookings.maintenance_requests(booking_id);

CREATE INDEX IF NOT EXISTS idx_maintenance_guest_id
    ON bookings.maintenance_requests(guest_id);

-- Constraint: either tenancy_id or booking_id must be set
ALTER TABLE bookings.maintenance_requests
    ADD CONSTRAINT chk_tenancy_or_booking
    CHECK (tenancy_id IS NOT NULL OR booking_id IS NOT NULL);
