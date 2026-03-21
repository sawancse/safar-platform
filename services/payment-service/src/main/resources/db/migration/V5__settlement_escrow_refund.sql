-- Settlement plans for split payments
CREATE TABLE IF NOT EXISTS payments.settlement_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL,
    payment_id UUID,
    total_amount_paise BIGINT NOT NULL,
    currency VARCHAR(3) DEFAULT 'INR',
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ
);
CREATE INDEX idx_settlement_plans_booking ON payments.settlement_plans(booking_id);

-- Individual settlement lines (one per recipient)
CREATE TABLE IF NOT EXISTS payments.settlement_lines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id UUID NOT NULL REFERENCES payments.settlement_plans(id),
    recipient_type VARCHAR(30) NOT NULL,
    recipient_id UUID,
    amount_paise BIGINT NOT NULL,
    commission_rate DECIMAL(5,4),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    payout_method VARCHAR(20),
    scheduled_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    failure_reason VARCHAR(500),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_settlement_lines_plan ON payments.settlement_lines(plan_id);
CREATE INDEX idx_settlement_lines_status ON payments.settlement_lines(status);

-- Escrow tracking
CREATE TABLE IF NOT EXISTS payments.escrow_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL,
    amount_paise BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'HELD',
    milestone VARCHAR(50),
    held_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    released_at TIMESTAMPTZ,
    released_to VARCHAR(30),
    released_amount_paise BIGINT DEFAULT 0,
    notes TEXT
);
CREATE INDEX idx_escrow_booking ON payments.escrow_entries(booking_id);
CREATE INDEX idx_escrow_status ON payments.escrow_entries(status);

-- Refund records
CREATE TABLE IF NOT EXISTS payments.refunds (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id UUID NOT NULL,
    booking_id UUID,
    gateway_refund_id VARCHAR(100),
    amount_paise BIGINT NOT NULL,
    reason VARCHAR(50) NOT NULL,
    refund_type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'INITIATED',
    fx_lock_id UUID,
    original_currency VARCHAR(3),
    original_amount BIGINT,
    initiated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ,
    failure_reason VARCHAR(500)
);
CREATE INDEX idx_refunds_payment ON payments.refunds(payment_id);
CREATE INDEX idx_refunds_booking ON payments.refunds(booking_id);

-- Webhook dedup table
CREATE TABLE IF NOT EXISTS payments.processed_webhooks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    gateway_event_id VARCHAR(200) NOT NULL UNIQUE,
    gateway VARCHAR(20) NOT NULL,
    event_type VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'PROCESSING',
    processed_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_webhooks_event_id ON payments.processed_webhooks(gateway_event_id);

-- Ledger entries (double-entry accounting)
CREATE TABLE IF NOT EXISTS payments.ledger_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID,
    entry_type VARCHAR(30) NOT NULL,
    debit_account VARCHAR(50) NOT NULL,
    credit_account VARCHAR(50) NOT NULL,
    amount_paise BIGINT NOT NULL,
    description VARCHAR(500),
    reference_id UUID,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_ledger_booking ON payments.ledger_entries(booking_id);
CREATE INDEX idx_ledger_type ON payments.ledger_entries(entry_type);
