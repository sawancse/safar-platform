-- Host payout tracking: commission settlement between Safar and hosts
CREATE TABLE IF NOT EXISTS payments.host_payouts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenancy_id UUID NOT NULL,
    host_id UUID NOT NULL,
    invoice_id UUID NOT NULL,
    gross_amount_paise BIGINT NOT NULL,
    commission_rate_bps INT NOT NULL,
    commission_paise BIGINT NOT NULL,
    gst_on_commission_paise BIGINT NOT NULL,
    tds_amount_paise BIGINT NOT NULL DEFAULT 0,
    net_payout_paise BIGINT NOT NULL,
    payout_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    razorpay_transfer_id VARCHAR(100),
    razorpay_payout_id VARCHAR(100),
    payout_date DATE,
    settlement_period_start DATE,
    settlement_period_end DATE,
    failure_reason VARCHAR(500),
    retry_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_hp_host ON payments.host_payouts(host_id);
CREATE INDEX idx_hp_tenancy ON payments.host_payouts(tenancy_id);
CREATE INDEX idx_hp_invoice ON payments.host_payouts(invoice_id);
CREATE INDEX idx_hp_status ON payments.host_payouts(payout_status);
CREATE INDEX idx_hp_created ON payments.host_payouts(created_at);
