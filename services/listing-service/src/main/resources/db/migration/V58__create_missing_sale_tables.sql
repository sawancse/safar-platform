-- V58: Create all sale/builder tables that may be missing in production
-- (V51-V56 were skipped due to version conflict with pg_penalty_config)

CREATE TABLE IF NOT EXISTS sale_properties (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    seller_id UUID NOT NULL,
    seller_type VARCHAR(20) NOT NULL DEFAULT 'OWNER',
    linked_listing_id UUID,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    sale_property_type VARCHAR(30) NOT NULL,
    transaction_type VARCHAR(20) DEFAULT 'RESALE',
    address_line1 VARCHAR(500),
    address_line2 VARCHAR(500),
    locality VARCHAR(100),
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    pincode VARCHAR(6) NOT NULL,
    lat DECIMAL(9,6),
    lng DECIMAL(9,6),
    landmark VARCHAR(200),
    asking_price_paise BIGINT NOT NULL,
    price_per_sqft_paise BIGINT,
    price_negotiable BOOLEAN DEFAULT FALSE,
    maintenance_paise BIGINT,
    booking_amount_paise BIGINT,
    carpet_area_sqft INTEGER,
    built_up_area_sqft INTEGER,
    super_built_up_area_sqft INTEGER,
    plot_area_sqft INTEGER,
    area_unit VARCHAR(10) DEFAULT 'SQFT',
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
    possession_status VARCHAR(25) DEFAULT 'READY_TO_MOVE',
    possession_date DATE,
    builder_name VARCHAR(200),
    project_name VARCHAR(200),
    rera_id VARCHAR(100),
    rera_verified BOOLEAN DEFAULT FALSE,
    amenities TEXT[],
    water_supply VARCHAR(15),
    power_backup VARCHAR(10),
    gated_community BOOLEAN DEFAULT FALSE,
    corner_property BOOLEAN DEFAULT FALSE,
    vastu_compliant BOOLEAN DEFAULT FALSE,
    pet_allowed BOOLEAN DEFAULT FALSE,
    overlooking TEXT[],
    photos TEXT[],
    floor_plan_url VARCHAR(500),
    video_tour_url VARCHAR(500),
    brochure_url VARCHAR(500),
    status VARCHAR(20) DEFAULT 'DRAFT',
    featured BOOLEAN DEFAULT FALSE,
    verified BOOLEAN DEFAULT FALSE,
    views_count INTEGER DEFAULT 0,
    inquiries_count INTEGER DEFAULT 0,
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    approved_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_sale_props_seller ON sale_properties(seller_id);
CREATE INDEX IF NOT EXISTS idx_sale_props_city ON sale_properties(city);
CREATE INDEX IF NOT EXISTS idx_sale_props_locality ON sale_properties(locality);
CREATE INDEX IF NOT EXISTS idx_sale_props_status ON sale_properties(status);
CREATE INDEX IF NOT EXISTS idx_sale_props_type ON sale_properties(sale_property_type);
CREATE INDEX IF NOT EXISTS idx_sale_props_price ON sale_properties(asking_price_paise);
CREATE INDEX IF NOT EXISTS idx_sale_props_bedrooms ON sale_properties(bedrooms);
CREATE INDEX IF NOT EXISTS idx_sale_props_possession ON sale_properties(possession_status);
CREATE INDEX IF NOT EXISTS idx_sale_props_linked ON sale_properties(linked_listing_id);

CREATE TABLE IF NOT EXISTS property_inquiries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sale_property_id UUID NOT NULL REFERENCES sale_properties(id),
    buyer_id UUID NOT NULL,
    seller_id UUID NOT NULL,
    status VARCHAR(20) DEFAULT 'NEW',
    message TEXT,
    buyer_name VARCHAR(100),
    buyer_phone VARCHAR(15),
    buyer_email VARCHAR(200),
    preferred_visit_date DATE,
    preferred_visit_time VARCHAR(20),
    financing_type VARCHAR(15),
    budget_min_paise BIGINT,
    budget_max_paise BIGINT,
    notes TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_inquiries_property ON property_inquiries(sale_property_id);
CREATE INDEX IF NOT EXISTS idx_inquiries_buyer ON property_inquiries(buyer_id);
CREATE INDEX IF NOT EXISTS idx_inquiries_seller ON property_inquiries(seller_id);
CREATE INDEX IF NOT EXISTS idx_inquiries_status ON property_inquiries(status);

CREATE TABLE IF NOT EXISTS site_visits (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    inquiry_id UUID REFERENCES property_inquiries(id),
    sale_property_id UUID NOT NULL REFERENCES sale_properties(id),
    buyer_id UUID NOT NULL,
    seller_id UUID NOT NULL,
    scheduled_at TIMESTAMPTZ NOT NULL,
    duration_minutes INTEGER DEFAULT 30,
    status VARCHAR(20) DEFAULT 'REQUESTED',
    buyer_feedback TEXT,
    seller_feedback TEXT,
    rating INTEGER,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_visits_property ON site_visits(sale_property_id);
CREATE INDEX IF NOT EXISTS idx_visits_buyer ON site_visits(buyer_id);
CREATE INDEX IF NOT EXISTS idx_visits_seller ON site_visits(seller_id);
CREATE INDEX IF NOT EXISTS idx_visits_scheduled ON site_visits(scheduled_at);
CREATE INDEX IF NOT EXISTS idx_visits_status ON site_visits(status);

CREATE TABLE IF NOT EXISTS buyer_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE,
    preferred_cities TEXT[],
    preferred_localities TEXT[],
    budget_min_paise BIGINT,
    budget_max_paise BIGINT,
    preferred_bhk TEXT[],
    preferred_types TEXT[],
    financing_type VARCHAR(15),
    possession_timeline VARCHAR(20) DEFAULT 'FLEXIBLE',
    alerts_enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_buyer_profile_user ON buyer_profiles(user_id);

CREATE TABLE IF NOT EXISTS sale_price_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sale_property_id UUID NOT NULL REFERENCES sale_properties(id),
    price_paise BIGINT NOT NULL,
    price_per_sqft_paise BIGINT,
    changed_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_price_history_property ON sale_price_history(sale_property_id);
CREATE INDEX IF NOT EXISTS idx_price_history_date ON sale_price_history(changed_at);

CREATE TABLE IF NOT EXISTS locality_price_trends (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    city VARCHAR(100) NOT NULL,
    locality VARCHAR(100) NOT NULL,
    month DATE NOT NULL,
    avg_price_per_sqft_paise BIGINT,
    median_price_per_sqft_paise BIGINT,
    total_listings INTEGER DEFAULT 0,
    total_sold INTEGER DEFAULT 0,
    property_type VARCHAR(30),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(city, locality, month, property_type)
);

CREATE INDEX IF NOT EXISTS idx_locality_trends_city ON locality_price_trends(city, locality);
CREATE INDEX IF NOT EXISTS idx_locality_trends_month ON locality_price_trends(month);

CREATE TABLE IF NOT EXISTS builder_projects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    builder_id UUID NOT NULL,
    builder_name VARCHAR(200) NOT NULL,
    builder_logo_url VARCHAR(500),
    project_name VARCHAR(200) NOT NULL,
    tagline VARCHAR(300),
    description TEXT,
    rera_id VARCHAR(100),
    rera_verified BOOLEAN DEFAULT FALSE,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    locality VARCHAR(100),
    pincode VARCHAR(6) NOT NULL,
    lat DECIMAL(9,6),
    lng DECIMAL(9,6),
    address TEXT,
    total_units INTEGER,
    available_units INTEGER,
    total_towers INTEGER,
    total_floors_max INTEGER,
    project_status VARCHAR(25) NOT NULL DEFAULT 'UNDER_CONSTRUCTION',
    launch_date DATE,
    possession_date DATE,
    construction_progress_percent INTEGER DEFAULT 0,
    land_area_sqft INTEGER,
    project_area_sqft INTEGER,
    amenities TEXT[],
    master_plan_url VARCHAR(500),
    brochure_url VARCHAR(500),
    walkthrough_url VARCHAR(500),
    photos TEXT[],
    bank_approvals TEXT[],
    payment_plans TEXT,
    min_price_paise BIGINT,
    max_price_paise BIGINT,
    min_bhk INTEGER,
    max_bhk INTEGER,
    min_area_sqft INTEGER,
    max_area_sqft INTEGER,
    status VARCHAR(20) DEFAULT 'DRAFT',
    verified BOOLEAN DEFAULT FALSE,
    views_count INTEGER DEFAULT 0,
    inquiries_count INTEGER DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_builder_projects_builder ON builder_projects(builder_id);
CREATE INDEX IF NOT EXISTS idx_builder_projects_city ON builder_projects(city);
CREATE INDEX IF NOT EXISTS idx_builder_projects_locality ON builder_projects(locality);
CREATE INDEX IF NOT EXISTS idx_builder_projects_status ON builder_projects(status);
CREATE INDEX IF NOT EXISTS idx_builder_projects_project_status ON builder_projects(project_status);
CREATE INDEX IF NOT EXISTS idx_builder_projects_price ON builder_projects(min_price_paise, max_price_paise);

CREATE TABLE IF NOT EXISTS project_unit_types (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES builder_projects(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    bhk INTEGER NOT NULL,
    carpet_area_sqft INTEGER,
    built_up_area_sqft INTEGER,
    super_built_up_area_sqft INTEGER,
    base_price_paise BIGINT NOT NULL,
    floor_rise_paise BIGINT DEFAULT 0,
    facing_premium_paise BIGINT DEFAULT 0,
    premium_floors_from INTEGER,
    total_units INTEGER,
    available_units INTEGER,
    bathrooms INTEGER,
    balconies INTEGER,
    furnishing VARCHAR(20),
    floor_plan_url VARCHAR(500),
    unit_layout_url VARCHAR(500),
    photos TEXT[],
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_unit_types_project ON project_unit_types(project_id);
CREATE INDEX IF NOT EXISTS idx_unit_types_bhk ON project_unit_types(bhk);

CREATE TABLE IF NOT EXISTS construction_updates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES builder_projects(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    progress_percent INTEGER,
    photos TEXT[],
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_construction_updates_project ON construction_updates(project_id);
