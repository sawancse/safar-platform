-- PG Tenancy Agreement: digital rental agreement with dual e-sign
CREATE TABLE IF NOT EXISTS bookings.tenancy_agreements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenancy_id UUID NOT NULL,
    agreement_number VARCHAR(30) NOT NULL UNIQUE,
    tenant_name VARCHAR(200) NOT NULL,
    tenant_phone VARCHAR(20),
    tenant_email VARCHAR(200),
    tenant_aadhaar_last4 VARCHAR(4),
    host_name VARCHAR(200) NOT NULL,
    host_phone VARCHAR(20),
    property_address TEXT NOT NULL,
    room_description VARCHAR(500),
    move_in_date DATE NOT NULL,
    lock_in_period_months INT NOT NULL DEFAULT 0,
    notice_period_days INT NOT NULL,
    monthly_rent_paise BIGINT NOT NULL,
    security_deposit_paise BIGINT NOT NULL,
    maintenance_charges_paise BIGINT NOT NULL DEFAULT 0,
    agreement_text TEXT NOT NULL,
    terms_and_conditions TEXT,
    status VARCHAR(25) NOT NULL DEFAULT 'DRAFT',
    host_signed_at TIMESTAMPTZ,
    host_signature_ip VARCHAR(45),
    tenant_signed_at TIMESTAMPTZ,
    tenant_signature_ip VARCHAR(45),
    agreement_pdf_url VARCHAR(500),
    stamp_duty_paise BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_ta_tenancy FOREIGN KEY (tenancy_id) REFERENCES bookings.pg_tenancies(id)
);

CREATE INDEX idx_ta_tenancy ON bookings.tenancy_agreements(tenancy_id);
CREATE INDEX idx_ta_status ON bookings.tenancy_agreements(status);
CREATE SEQUENCE IF NOT EXISTS bookings.agreement_ref_seq START WITH 1000 INCREMENT BY 1;
