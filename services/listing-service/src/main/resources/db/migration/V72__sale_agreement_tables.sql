-- Sale Agreement service tables

-- Agreement requests
CREATE TABLE IF NOT EXISTS agreement_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    agreement_type VARCHAR(30) NOT NULL, -- SALE_AGREEMENT, SALE_DEED, RENTAL_AGREEMENT, LEAVE_LICENSE, PG_AGREEMENT
    property_id UUID, -- linked sale property
    listing_id UUID, -- linked rental listing
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT', -- DRAFT, STAMPED, PENDING_SIGN, SIGNED, PENDING_REGISTRATION, REGISTERED, DELIVERED
    package_type VARCHAR(20) NOT NULL DEFAULT 'BASIC', -- BASIC, ESTAMP, REGISTERED, PREMIUM
    party_details_json JSONB,
    property_details_json JSONB,
    clauses_json JSONB,
    stamp_duty_paise BIGINT DEFAULT 0,
    registration_fee_paise BIGINT DEFAULT 0,
    service_fees_paise BIGINT DEFAULT 0,
    total_paise BIGINT DEFAULT 0,
    draft_pdf_url TEXT,
    signed_pdf_url TEXT,
    registered_pdf_url TEXT,
    e_stamp_id VARCHAR(100),
    registration_number VARCHAR(100),
    payment_id UUID,
    payment_status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, PAID, REFUNDED
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX idx_agreement_user ON agreement_requests(user_id);
CREATE INDEX idx_agreement_status ON agreement_requests(status);

-- Agreement parties (buyer, seller, witnesses)
CREATE TABLE IF NOT EXISTS agreement_parties (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agreement_id UUID NOT NULL REFERENCES agreement_requests(id) ON DELETE CASCADE,
    party_type VARCHAR(20) NOT NULL, -- BUYER, SELLER, TENANT, LANDLORD, WITNESS
    full_name VARCHAR(200) NOT NULL,
    aadhaar_number VARCHAR(20),
    pan_number VARCHAR(15),
    address TEXT,
    phone VARCHAR(15),
    email VARCHAR(100),
    e_sign_status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, SIGNED, REJECTED
    e_signed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX idx_party_agreement ON agreement_parties(agreement_id);

-- Stamp duty config per state
CREATE TABLE IF NOT EXISTS stamp_duty_configs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    state VARCHAR(50) NOT NULL,
    agreement_type VARCHAR(30) NOT NULL,
    duty_percent DECIMAL(5,2) NOT NULL,
    registration_percent DECIMAL(5,2) DEFAULT 0,
    surcharge_percent DECIMAL(5,2) DEFAULT 0,
    effective_from DATE NOT NULL,
    active BOOLEAN DEFAULT TRUE
);
CREATE INDEX idx_stamp_duty_state ON stamp_duty_configs(state, agreement_type);
