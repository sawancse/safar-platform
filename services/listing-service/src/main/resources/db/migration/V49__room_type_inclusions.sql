-- V49: Room Type Inclusions & Perks system
-- Allows hosts to configure per-room-type inclusions (meals, discounts, perks, add-ons)

CREATE TABLE listings.room_type_inclusions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_type_id UUID NOT NULL REFERENCES listings.room_types(id) ON DELETE CASCADE,
    category VARCHAR(30) NOT NULL,
    -- Categories: MEAL, DISCOUNT, FLEXIBILITY, WELLNESS, TRANSPORT, AMENITY, EXPERIENCE
    name VARCHAR(100) NOT NULL,
    description TEXT,
    inclusion_mode VARCHAR(20) NOT NULL DEFAULT 'INCLUDED',
    -- Modes: INCLUDED (in room rate), PAID_ADDON (guest pays extra), COMPLIMENTARY (free extra)
    charge_paise BIGINT DEFAULT 0,
    charge_type VARCHAR(20) DEFAULT 'PER_STAY',
    -- Charge types: PER_NIGHT, PER_STAY, PER_PERSON, PER_HOUR, PER_USE
    discount_percent INTEGER DEFAULT 0,
    -- For DISCOUNT category: e.g., 10 = 10% off F&B
    terms VARCHAR(500),
    -- Conditions: "Subject to availability", "Valid for stays > 3 nights"
    is_highlight BOOLEAN DEFAULT FALSE,
    -- Show prominently on listing card
    sort_order INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_rti_room_type ON listings.room_type_inclusions(room_type_id);
CREATE INDEX idx_rti_category ON listings.room_type_inclusions(room_type_id, category);
