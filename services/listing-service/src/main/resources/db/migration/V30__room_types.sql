CREATE TABLE listings.room_types (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id UUID NOT NULL REFERENCES listings.listings(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    count INTEGER NOT NULL DEFAULT 1,
    base_price_paise BIGINT NOT NULL,
    max_guests INTEGER NOT NULL DEFAULT 2,
    bed_type VARCHAR(50),
    bed_count INTEGER DEFAULT 1,
    area_sqft INTEGER,
    amenities TEXT,
    sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX idx_room_types_listing ON listings.room_types(listing_id);

CREATE TABLE listings.room_type_availability (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_type_id UUID NOT NULL REFERENCES listings.room_types(id) ON DELETE CASCADE,
    date DATE NOT NULL,
    available_count INTEGER NOT NULL,
    price_override_paise BIGINT,
    min_stay_nights INTEGER DEFAULT 1,
    UNIQUE(room_type_id, date)
);
CREATE INDEX idx_rta_room_date ON listings.room_type_availability(room_type_id, date);
