-- =============================================
-- V35: Settlement enhancement — refund deadline, inspection checklist, dispute workflow
-- =============================================

-- 1. Settlement enhancements
ALTER TABLE bookings.tenancy_settlements ADD COLUMN refund_deadline_date DATE;
ALTER TABLE bookings.tenancy_settlements ADD COLUMN refund_deadline_days INT NOT NULL DEFAULT 21;
ALTER TABLE bookings.tenancy_settlements ADD COLUMN is_overdue BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE bookings.tenancy_settlements ADD COLUMN dispute_reason TEXT;
ALTER TABLE bookings.tenancy_settlements ADD COLUMN dispute_raised_at TIMESTAMPTZ;
ALTER TABLE bookings.tenancy_settlements ADD COLUMN admin_override_at TIMESTAMPTZ;
ALTER TABLE bookings.tenancy_settlements ADD COLUMN admin_override_by UUID;
ALTER TABLE bookings.tenancy_settlements ADD COLUMN admin_override_notes TEXT;
ALTER TABLE bookings.tenancy_settlements ADD COLUMN tenant_bank_account VARCHAR(30);
ALTER TABLE bookings.tenancy_settlements ADD COLUMN tenant_ifsc VARCHAR(15);
ALTER TABLE bookings.tenancy_settlements ADD COLUMN tenant_upi_id VARCHAR(100);
ALTER TABLE bookings.tenancy_settlements ADD COLUMN refund_proof_url VARCHAR(500);
ALTER TABLE bookings.tenancy_settlements ALTER COLUMN status TYPE VARCHAR(30);

-- 2. Deduction dispute fields
ALTER TABLE bookings.settlement_deductions ADD COLUMN disputed BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE bookings.settlement_deductions ADD COLUMN dispute_reason VARCHAR(500);
ALTER TABLE bookings.settlement_deductions ADD COLUMN dispute_resolved BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE bookings.settlement_deductions ADD COLUMN admin_decision VARCHAR(20);
ALTER TABLE bookings.settlement_deductions ADD COLUMN admin_adjusted_paise BIGINT;
ALTER TABLE bookings.settlement_deductions ADD COLUMN admin_notes VARCHAR(500);

-- 3. Inspection checklist items
CREATE TABLE IF NOT EXISTS bookings.inspection_checklist_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    settlement_id UUID NOT NULL,
    area VARCHAR(50) NOT NULL,
    item_name VARCHAR(200) NOT NULL,
    condition VARCHAR(20) NOT NULL,
    damage_description VARCHAR(500),
    photo_urls TEXT,
    deduction_paise BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_ici_settlement FOREIGN KEY (settlement_id)
        REFERENCES bookings.tenancy_settlements(id) ON DELETE CASCADE
);

CREATE INDEX idx_ici_settlement ON bookings.inspection_checklist_items(settlement_id);
CREATE INDEX idx_ts_overdue ON bookings.tenancy_settlements(is_overdue) WHERE is_overdue = TRUE;
CREATE INDEX idx_ts_refund_deadline ON bookings.tenancy_settlements(refund_deadline_date);
