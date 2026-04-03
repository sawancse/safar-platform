-- V56: Builder Projects for new construction sales
CREATE TABLE builder_projects (
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
    payment_plans TEXT, -- JSON string

    -- Computed price range from unit types
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

CREATE INDEX idx_builder_projects_builder ON builder_projects(builder_id);
CREATE INDEX idx_builder_projects_city ON builder_projects(city);
CREATE INDEX idx_builder_projects_locality ON builder_projects(locality);
CREATE INDEX idx_builder_projects_status ON builder_projects(status);
CREATE INDEX idx_builder_projects_project_status ON builder_projects(project_status);
CREATE INDEX idx_builder_projects_price ON builder_projects(min_price_paise, max_price_paise);

-- Unit types within a project
CREATE TABLE project_unit_types (
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

CREATE INDEX idx_unit_types_project ON project_unit_types(project_id);
CREATE INDEX idx_unit_types_bhk ON project_unit_types(bhk);

-- Construction progress updates
CREATE TABLE construction_updates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES builder_projects(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    progress_percent INTEGER,
    photos TEXT[],
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_construction_updates_project ON construction_updates(project_id);
