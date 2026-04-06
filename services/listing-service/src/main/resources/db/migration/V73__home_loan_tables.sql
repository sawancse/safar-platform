-- Home Loan service tables

-- Partner banks
CREATE TABLE IF NOT EXISTS partner_banks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    bank_name VARCHAR(100) NOT NULL,
    logo_url TEXT,
    min_interest_rate DECIMAL(5,2) NOT NULL,
    max_interest_rate DECIMAL(5,2) NOT NULL,
    processing_fee_percent DECIMAL(5,2) DEFAULT 0.5,
    max_tenure_months INTEGER DEFAULT 360,
    max_ltv_percent INTEGER DEFAULT 80,
    min_loan_amount_paise BIGINT DEFAULT 500000000, -- 5L
    max_loan_amount_paise BIGINT DEFAULT 500000000000, -- 50Cr
    min_income_paise BIGINT DEFAULT 2500000, -- 25K
    special_offers TEXT,
    commission_percent DECIMAL(5,2) DEFAULT 1.0,
    features_json JSONB,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Loan eligibility checks
CREATE TABLE IF NOT EXISTS loan_eligibilities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    employment_type VARCHAR(20) NOT NULL, -- SALARIED, SELF_EMPLOYED, BUSINESS
    monthly_income_paise BIGINT NOT NULL,
    current_emis_paise BIGINT DEFAULT 0,
    desired_loan_amount_paise BIGINT NOT NULL,
    desired_tenure_months INTEGER DEFAULT 240,
    max_eligible_amount_paise BIGINT,
    max_emi_paise BIGINT,
    calculated_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX idx_eligibility_user ON loan_eligibilities(user_id);

-- Loan applications
CREATE TABLE IF NOT EXISTS loan_applications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    eligibility_id UUID REFERENCES loan_eligibilities(id),
    bank_id UUID NOT NULL REFERENCES partner_banks(id),
    property_id UUID, -- linked sale property
    loan_amount_paise BIGINT NOT NULL,
    tenure_months INTEGER NOT NULL,
    interest_rate DECIMAL(5,2),
    status VARCHAR(30) NOT NULL DEFAULT 'APPLIED', -- APPLIED, DOCUMENTS_PENDING, UNDER_REVIEW, SANCTIONED, DISBURSED, REJECTED
    sanctioned_amount_paise BIGINT,
    sanction_letter_url TEXT,
    rejection_reason TEXT,
    applicant_name VARCHAR(200),
    applicant_phone VARCHAR(15),
    applicant_email VARCHAR(100),
    applied_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX idx_loan_app_user ON loan_applications(user_id);
CREATE INDEX idx_loan_app_status ON loan_applications(status);

-- Loan documents
CREATE TABLE IF NOT EXISTS loan_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id UUID NOT NULL REFERENCES loan_applications(id) ON DELETE CASCADE,
    document_type VARCHAR(30) NOT NULL, -- ID_PROOF, INCOME_PROOF, BANK_STATEMENT, PROPERTY_DOCS, ITR, FORM16, SALARY_SLIP, ADDRESS_PROOF
    file_url TEXT NOT NULL,
    verification_status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, VERIFIED, REJECTED
    remarks TEXT,
    uploaded_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX idx_loan_doc_app ON loan_documents(application_id);
