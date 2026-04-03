-- Move-out settlement: tracks deductions, refund calculation, dual approval
CREATE TABLE IF NOT EXISTS bookings.tenancy_settlements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenancy_id UUID NOT NULL,
    settlement_ref VARCHAR(30) UNIQUE,
    move_out_date DATE NOT NULL,
    inspection_date DATE,
    inspection_notes TEXT,
    security_deposit_paise BIGINT NOT NULL,
    unpaid_rent_paise BIGINT NOT NULL DEFAULT 0,
    unpaid_utilities_paise BIGINT NOT NULL DEFAULT 0,
    damage_deduction_paise BIGINT NOT NULL DEFAULT 0,
    late_penalty_paise BIGINT NOT NULL DEFAULT 0,
    other_deductions_paise BIGINT NOT NULL DEFAULT 0,
    other_deductions_note VARCHAR(500),
    total_deductions_paise BIGINT NOT NULL DEFAULT 0,
    refund_amount_paise BIGINT NOT NULL DEFAULT 0,
    additional_due_paise BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'INITIATED',
    approved_by_host_at TIMESTAMPTZ,
    approved_by_tenant_at TIMESTAMPTZ,
    razorpay_refund_id VARCHAR(100),
    refunded_at TIMESTAMPTZ,
    settlement_pdf_url VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_ts_tenancy FOREIGN KEY (tenancy_id) REFERENCES bookings.pg_tenancies(id)
);

CREATE TABLE IF NOT EXISTS bookings.settlement_deductions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    settlement_id UUID NOT NULL,
    category VARCHAR(30) NOT NULL,
    description VARCHAR(500) NOT NULL,
    amount_paise BIGINT NOT NULL,
    evidence_url VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_sd_settlement FOREIGN KEY (settlement_id) REFERENCES bookings.tenancy_settlements(id)
);

CREATE INDEX idx_tsettle_tenancy ON bookings.tenancy_settlements(tenancy_id);
CREATE INDEX idx_tsettle_status ON bookings.tenancy_settlements(status);
CREATE INDEX idx_sd_settlement ON bookings.settlement_deductions(settlement_id);
CREATE SEQUENCE IF NOT EXISTS bookings.settlement_ref_seq START WITH 1000 INCREMENT BY 1;
