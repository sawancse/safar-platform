-- Home Interiors tables

-- Interior designers
CREATE TABLE IF NOT EXISTS interior_designers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name VARCHAR(200) NOT NULL,
    phone VARCHAR(15),
    email VARCHAR(100),
    city VARCHAR(100),
    experience_years INTEGER DEFAULT 0,
    specializations TEXT[],
    portfolio_urls TEXT[],
    projects_completed INTEGER DEFAULT 0,
    rating DECIMAL(3,2) DEFAULT 0,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Interior projects
CREATE TABLE IF NOT EXISTS interior_projects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    designer_id UUID REFERENCES interior_designers(id),
    project_manager_id UUID,
    project_type VARCHAR(30) NOT NULL, -- MODULAR_KITCHEN, WARDROBE, FULL_ROOM, FULL_HOME, RENOVATION
    property_type VARCHAR(30), -- APARTMENT, VILLA, INDEPENDENT_HOUSE
    property_address TEXT,
    city VARCHAR(100),
    state VARCHAR(50),
    pincode VARCHAR(10),
    room_count INTEGER DEFAULT 1,
    budget_min_paise BIGINT,
    budget_max_paise BIGINT,
    quoted_amount_paise BIGINT,
    status VARCHAR(30) NOT NULL DEFAULT 'CONSULTATION_BOOKED', -- CONSULTATION_BOOKED, MEASUREMENT_DONE, DESIGN_IN_PROGRESS, DESIGN_APPROVED, MATERIAL_SELECTED, QUOTE_APPROVED, EXECUTION, QC_IN_PROGRESS, COMPLETED, CANCELLED
    consultation_date DATE,
    start_date DATE,
    estimated_completion_date DATE,
    actual_completion_date DATE,
    warranty_expires_at DATE,
    payment_status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX idx_interior_user ON interior_projects(user_id);
CREATE INDEX idx_interior_status ON interior_projects(status);

-- Room designs
CREATE TABLE IF NOT EXISTS room_designs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES interior_projects(id) ON DELETE CASCADE,
    room_type VARCHAR(20) NOT NULL, -- LIVING, BEDROOM, KITCHEN, BATHROOM, DINING, STUDY, BALCONY
    area_sqft INTEGER,
    design_style VARCHAR(30), -- MODERN, CONTEMPORARY, TRADITIONAL, MINIMALIST, INDUSTRIAL
    render_3d_url TEXT,
    floor_plan_url TEXT,
    mood_board_url TEXT,
    current_photos TEXT[],
    status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, DESIGNED, APPROVED, EXECUTED
    created_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX idx_room_project ON room_designs(project_id);

-- Material catalog
CREATE TABLE IF NOT EXISTS materials_catalog (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category VARCHAR(30) NOT NULL, -- FLOORING, WALL, COUNTERTOP, CABINET, HARDWARE, LIGHTING, PAINT
    material_name VARCHAR(200) NOT NULL,
    brand VARCHAR(100),
    finish VARCHAR(50),
    unit_price_paise BIGINT,
    unit VARCHAR(20) DEFAULT 'SQFT', -- SQFT, UNIT, METER, SET
    image_url TEXT,
    specifications_json JSONB,
    active BOOLEAN DEFAULT TRUE
);

-- Material selections per project
CREATE TABLE IF NOT EXISTS material_selections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES interior_projects(id) ON DELETE CASCADE,
    room_design_id UUID REFERENCES room_designs(id),
    material_id UUID REFERENCES materials_catalog(id),
    category VARCHAR(30) NOT NULL,
    material_name VARCHAR(200),
    brand VARCHAR(100),
    quantity INTEGER DEFAULT 1,
    unit_price_paise BIGINT,
    total_price_paise BIGINT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Interior quotes
CREATE TABLE IF NOT EXISTS interior_quotes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES interior_projects(id) ON DELETE CASCADE,
    version INTEGER DEFAULT 1,
    material_cost_paise BIGINT DEFAULT 0,
    labor_cost_paise BIGINT DEFAULT 0,
    hardware_cost_paise BIGINT DEFAULT 0,
    overhead_paise BIGINT DEFAULT 0,
    discount_paise BIGINT DEFAULT 0,
    total_paise BIGINT DEFAULT 0,
    valid_until DATE,
    status VARCHAR(20) DEFAULT 'DRAFT', -- DRAFT, SENT, APPROVED, EXPIRED
    quote_doc_url TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Project milestones
CREATE TABLE IF NOT EXISTS project_milestones (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES interior_projects(id) ON DELETE CASCADE,
    milestone_name VARCHAR(200) NOT NULL,
    description TEXT,
    scheduled_date DATE,
    completed_date DATE,
    status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, IN_PROGRESS, COMPLETED, DELAYED
    photos TEXT[],
    payment_linked BOOLEAN DEFAULT FALSE,
    payment_amount_paise BIGINT DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX idx_milestone_project ON project_milestones(project_id);

-- Quality checks
CREATE TABLE IF NOT EXISTS quality_checks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES interior_projects(id) ON DELETE CASCADE,
    milestone_id UUID REFERENCES project_milestones(id),
    checkpoint_name VARCHAR(200) NOT NULL,
    category VARCHAR(30), -- MATERIAL, FINISH, ALIGNMENT, HARDWARE, ELECTRICAL, PLUMBING
    status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, PASS, FAIL, REWORK
    inspector_notes TEXT,
    photos TEXT[],
    inspected_at TIMESTAMPTZ
);
CREATE INDEX idx_qc_project ON quality_checks(project_id);
