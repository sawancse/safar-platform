-- V23: Multi-room-type booking + guest list
-- Allows booking multiple different room types in a single booking
-- And adding guest details per room

-- Room selections per booking (replaces single roomTypeId)
CREATE TABLE bookings.booking_room_selections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL REFERENCES bookings.bookings(id) ON DELETE CASCADE,
    room_type_id UUID NOT NULL,
    room_type_name VARCHAR(255) NOT NULL,
    count INTEGER NOT NULL DEFAULT 1,
    price_per_unit_paise BIGINT NOT NULL,
    total_paise BIGINT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_brs_booking ON bookings.booking_room_selections(booking_id);

-- Guest list per booking
CREATE TABLE bookings.booking_guests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL REFERENCES bookings.bookings(id) ON DELETE CASCADE,
    full_name VARCHAR(200) NOT NULL,
    email VARCHAR(200),
    phone VARCHAR(20),
    age INTEGER,
    id_type VARCHAR(30),  -- AADHAAR, PASSPORT, DRIVING_LICENSE, VOTER_ID
    id_number VARCHAR(50),
    room_assignment VARCHAR(100),  -- e.g. "Deluxe Room 1" or room_type_name
    is_primary BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_bg_booking ON bookings.booking_guests(booking_id);
