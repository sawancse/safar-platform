-- V22: Booking Inclusions — tracks which inclusions/perks the guest selected
-- Denormalized from listing-service room_type_inclusions for booking immutability

CREATE TABLE bookings.booking_inclusions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL REFERENCES bookings.bookings(id) ON DELETE CASCADE,
    inclusion_id UUID NOT NULL,
    -- Denormalized fields (snapshot at booking time)
    category VARCHAR(30) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    inclusion_mode VARCHAR(20) NOT NULL,
    charge_paise BIGINT DEFAULT 0,
    charge_type VARCHAR(20) DEFAULT 'PER_STAY',
    discount_percent INTEGER DEFAULT 0,
    terms VARCHAR(500),
    -- Booking-specific
    quantity INTEGER DEFAULT 1,
    total_paise BIGINT DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_bi_booking ON bookings.booking_inclusions(booking_id);

-- Add inclusions total to bookings table
ALTER TABLE bookings.bookings ADD COLUMN inclusions_total_paise BIGINT DEFAULT 0;
