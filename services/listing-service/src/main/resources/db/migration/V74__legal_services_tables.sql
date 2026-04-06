-- Legal Services tables

-- Advocates
CREATE TABLE IF NOT EXISTS advocates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name VARCHAR(200) NOT NULL,
    phone VARCHAR(15),
    email VARCHAR(100),
    bar_council_number VARCHAR(50),
    specializations TEXT[],
    city VARCHAR(100),
    state VARCHAR(50),
    cases_completed INTEGER DEFAULT 0,
    rating DECIMAL(3,2) DEFAULT 0,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Legal cases
CREATE TABLE IF NOT EXISTS legal_cases (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    property_id UUID,
    package_type VARCHAR(30) NOT NULL, -- TITLE_SEARCH, DUE_DILIGENCE, BUYER_ASSIST, PREMIUM
    status VARCHAR(30) NOT NULL DEFAULT 'CREATED', -- CREATED, DOCUMENTS_UPLOADED, ASSIGNED, IN_PROGRESS, REPORT_READY, CONSULTATION_DONE, CLOSED
    advocate_id UUID REFERENCES advocates(id),
    property_address TEXT,
    property_city VARCHAR(100),
    property_state VARCHAR(50),
    survey_number VARCHAR(100),
    risk_level VARCHAR(10), -- GREEN, YELLOW, RED
    findings_json JSONB,
    report_pdf_url TEXT,
    service_fees_paise BIGINT DEFAULT 0,
    payment_id UUID,
    payment_status VARCHAR(20) DEFAULT 'PENDING',
    due_date DATE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX idx_legal_case_user ON legal_cases(user_id);
CREATE INDEX idx_legal_case_status ON legal_cases(status);

-- Legal case documents
CREATE TABLE IF NOT EXISTS legal_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    case_id UUID NOT NULL REFERENCES legal_cases(id) ON DELETE CASCADE,
    document_type VARCHAR(30) NOT NULL, -- TITLE_DEED, SALE_DEED, TAX_RECEIPT, EC, KHATA, APPROVAL, PLAN, OTHER
    file_url TEXT NOT NULL,
    verification_status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, VERIFIED, ISSUE_FOUND, NOT_AVAILABLE
    remarks TEXT,
    uploaded_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX idx_legal_doc_case ON legal_documents(case_id);

-- Legal verifications
CREATE TABLE IF NOT EXISTS legal_verifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    case_id UUID NOT NULL REFERENCES legal_cases(id) ON DELETE CASCADE,
    verification_type VARCHAR(30) NOT NULL, -- TITLE_CHAIN, ENCUMBRANCE, GOVT_APPROVAL, LITIGATION, TAX, SURVEY
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, IN_PROGRESS, CLEAN, ISSUE_FOUND
    findings_json JSONB,
    verified_at TIMESTAMPTZ
);
CREATE INDEX idx_verification_case ON legal_verifications(case_id);

-- Legal consultations
CREATE TABLE IF NOT EXISTS legal_consultations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    case_id UUID NOT NULL REFERENCES legal_cases(id) ON DELETE CASCADE,
    advocate_id UUID NOT NULL REFERENCES advocates(id),
    scheduled_at TIMESTAMPTZ NOT NULL,
    duration_minutes INTEGER DEFAULT 30,
    meeting_url TEXT,
    notes TEXT,
    status VARCHAR(20) DEFAULT 'SCHEDULED', -- SCHEDULED, COMPLETED, CANCELLED
    created_at TIMESTAMPTZ DEFAULT NOW()
);
