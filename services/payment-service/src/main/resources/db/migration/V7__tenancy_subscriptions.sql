-- Tenancy subscription tracking for PG recurring rent via Razorpay
CREATE TABLE IF NOT EXISTS payments.tenancy_subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenancy_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    razorpay_plan_id VARCHAR(100) NOT NULL,
    razorpay_subscription_id VARCHAR(100) NOT NULL UNIQUE,
    amount_paise BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'CREATED',
    start_at TIMESTAMPTZ,
    current_start TIMESTAMPTZ,
    current_end TIMESTAMPTZ,
    charge_attempts INT NOT NULL DEFAULT 0,
    last_charged_at TIMESTAMPTZ,
    last_failed_at TIMESTAMPTZ,
    failure_reason VARCHAR(500),
    cancelled_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ts_tenancy ON payments.tenancy_subscriptions(tenancy_id);
CREATE INDEX idx_ts_status ON payments.tenancy_subscriptions(status);
CREATE INDEX idx_ts_rzp_sub ON payments.tenancy_subscriptions(razorpay_subscription_id);
