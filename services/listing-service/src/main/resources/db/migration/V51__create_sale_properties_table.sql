-- V51: Sale Properties table for buy/sell marketplace
CREATE TABLE sale_properties (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    seller_id UUID NOT NULL,
    seller_type VARCHAR(20) NOT NULL DEFAULT 'OWNER',
    linked_listing_id UUID,

    -- Basic Info
    title VARCHAR(200) NOT NULL,
    description TEXT,
    sale_property_type VARCHAR(30) NOT NULL,
    transaction_type VARCHAR(20) DEFAULT 'RESALE',

    -- Location
    address_line1 VARCHAR(500),
    address_line2 VARCHAR(500),
    locality VARCHAR(100),
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    pincode VARCHAR(6) NOT NULL,
    lat DECIMAL(9,6),
    lng DECIMAL(9,6),
    landmark VARCHAR(200),

    -- Pricing
    asking_price_paise BIGINT NOT NULL,
    price_per_sqft_paise BIGINT,
    price_negotiable BOOLEAN DEFAULT FALSE,
    maintenance_paise BIGINT,
    booking_amount_paise BIGINT,

    -- Area & Dimensions
    carpet_area_sqft INTEGER,
    built_up_area_sqft INTEGER,
    super_built_up_area_sqft INTEGER,
    plot_area_sqft INTEGER,
    area_unit VARCHAR(10) DEFAULT 'SQFT',

    -- Configuration
    bedrooms INTEGER,
    bathrooms INTEGER,
    balconies INTEGER,
    floor_number INTEGER,
    total_floors INTEGER,
    facing VARCHAR(20),
    property_age_years INTEGER,
    furnishing VARCHAR(20),
    parking_covered INTEGER DEFAULT 0,
    parking_open INTEGER DEFAULT 0,

    -- Construction & Legal
    possession_status VARCHAR(25) DEFAULT 'READY_TO_MOVE',
    possession_date DATE,
    builder_name VARCHAR(200),
    project_name VARCHAR(200),
    rera_id VARCHAR(100),
    rera_verified BOOLEAN DEFAULT FALSE,

    -- Features
    amenities TEXT[],
    water_supply VARCHAR(15),
    power_backup VARCHAR(10),
    gated_community BOOLEAN DEFAULT FALSE,
    corner_property BOOLEAN DEFAULT FALSE,
    vastu_compliant BOOLEAN DEFAULT FALSE,
    pet_allowed BOOLEAN DEFAULT FALSE,
    overlooking TEXT[],

    -- Media
    photos TEXT[],
    floor_plan_url VARCHAR(500),
    video_tour_url VARCHAR(500),
    brochure_url VARCHAR(500),

    -- Status
    status VARCHAR(20) DEFAULT 'DRAFT',
    featured BOOLEAN DEFAULT FALSE,
    verified BOOLEAN DEFAULT FALSE,
    views_count INTEGER DEFAULT 0,
    inquiries_count INTEGER DEFAULT 0,
    expires_at TIMESTAMPTZ,

    -- Audit
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    approved_at TIMESTAMPTZ
);

CREATE INDEX idx_sale_props_seller ON sale_properties(seller_id);
CREATE INDEX idx_sale_props_city ON sale_properties(city);
CREATE INDEX idx_sale_props_locality ON sale_properties(locality);
CREATE INDEX idx_sale_props_status ON sale_properties(status);
CREATE INDEX idx_sale_props_type ON sale_properties(sale_property_type);
CREATE INDEX idx_sale_props_price ON sale_properties(asking_price_paise);
CREATE INDEX idx_sale_props_bedrooms ON sale_properties(bedrooms);
CREATE INDEX idx_sale_props_possession ON sale_properties(possession_status);
CREATE INDEX idx_sale_props_linked ON sale_properties(linked_listing_id);
