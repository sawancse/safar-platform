-- Room type additions: stay mode, sharing type, room variant, beds
ALTER TABLE listings.room_types ADD COLUMN IF NOT EXISTS stay_mode VARCHAR(20) DEFAULT 'NIGHTLY';
ALTER TABLE listings.room_types ADD COLUMN IF NOT EXISTS sharing_type VARCHAR(20);
ALTER TABLE listings.room_types ADD COLUMN IF NOT EXISTS room_variant VARCHAR(20);
ALTER TABLE listings.room_types ADD COLUMN IF NOT EXISTS total_beds INTEGER;
ALTER TABLE listings.room_types ADD COLUMN IF NOT EXISTS occupied_beds INTEGER DEFAULT 0;

-- Gender policy on listings
ALTER TABLE listings.listings ADD COLUMN IF NOT EXISTS gender_policy VARCHAR(20);

-- PG Packages table
CREATE TABLE IF NOT EXISTS listings.pg_packages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id UUID NOT NULL REFERENCES listings.listings(id),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    monthly_price_paise BIGINT NOT NULL,
    includes_meals BOOLEAN DEFAULT false,
    includes_laundry BOOLEAN DEFAULT false,
    includes_wifi BOOLEAN DEFAULT false,
    includes_housekeeping BOOLEAN DEFAULT false,
    sort_order INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_pg_packages_listing_id ON listings.pg_packages(listing_id);

-- Hourly Pricing Plans table
CREATE TABLE IF NOT EXISTS listings.hourly_pricing_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_type_id UUID NOT NULL REFERENCES listings.room_types(id),
    slot_hours INTEGER NOT NULL,
    price_paise BIGINT NOT NULL,
    available_from TIME DEFAULT '06:00:00',
    available_until TIME DEFAULT '22:00:00'
);

CREATE INDEX IF NOT EXISTS idx_hourly_pricing_room_type ON listings.hourly_pricing_plans(room_type_id);
