-- V15: Host KYC verification
CREATE TABLE IF NOT EXISTS users.host_kyc (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED',

    -- Step 1: Identity
    full_legal_name VARCHAR(200),
    date_of_birth DATE,
    aadhaar_number VARCHAR(12),
    aadhaar_verified BOOLEAN NOT NULL DEFAULT false,
    pan_number VARCHAR(10),
    pan_verified BOOLEAN NOT NULL DEFAULT false,

    -- Step 2: Address
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(100),
    pincode VARCHAR(6),

    -- Step 3: Bank details (for payouts)
    bank_account_name VARCHAR(200),
    bank_account_number VARCHAR(20),
    bank_ifsc VARCHAR(11),
    bank_name VARCHAR(100),
    bank_verified BOOLEAN NOT NULL DEFAULT false,

    -- Step 4: Business (optional, for commercial hosts)
    gstin VARCHAR(15),
    gst_verified BOOLEAN NOT NULL DEFAULT false,
    business_name VARCHAR(200),
    business_type VARCHAR(50),

    -- Meta
    submitted_at TIMESTAMPTZ,
    verified_at TIMESTAMPTZ,
    rejected_at TIMESTAMPTZ,
    rejection_reason TEXT,
    verified_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_host_kyc_user_id ON users.host_kyc (user_id);
CREATE INDEX IF NOT EXISTS idx_host_kyc_status ON users.host_kyc (status);
