-- Aashray Phase 2: Case Management for displaced persons housing
CREATE TABLE IF NOT EXISTS listings.aashray_cases (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    case_number VARCHAR(20) NOT NULL UNIQUE,
    seeker_name VARCHAR(255) NOT NULL,
    seeker_phone VARCHAR(255), -- encrypted
    seeker_email VARCHAR(255),
    family_size INT NOT NULL DEFAULT 1,
    children INT NOT NULL DEFAULT 0,
    elderly INT NOT NULL DEFAULT 0,
    current_city VARCHAR(100),
    preferred_city VARCHAR(100) NOT NULL,
    preferred_locality VARCHAR(255),
    budget_max_paise BIGINT NOT NULL DEFAULT 0,
    languages_spoken VARCHAR(500),
    special_needs TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    priority VARCHAR(10) NOT NULL DEFAULT 'MEDIUM',
    referral_source VARCHAR(255),
    matched_listing_id UUID,
    assigned_ngo_id UUID,
    need_by_date DATE,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_aashray_cases_status ON listings.aashray_cases(status);
CREATE INDEX idx_aashray_cases_priority ON listings.aashray_cases(priority);
CREATE INDEX idx_aashray_cases_city ON listings.aashray_cases(preferred_city);
CREATE INDEX idx_aashray_cases_ngo ON listings.aashray_cases(assigned_ngo_id);

CREATE SEQUENCE IF NOT EXISTS listings.aashray_case_seq START WITH 1000 INCREMENT BY 1;
