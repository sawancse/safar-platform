-- FX rate cache and booking FX lock
CREATE TABLE IF NOT EXISTS payments.fx_rates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    base_currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    target_currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    rate DECIMAL(16,6) NOT NULL,
    margin_rate DECIMAL(16,6) NOT NULL,
    fetched_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_fx_rates_currencies ON payments.fx_rates(base_currency, target_currency, fetched_at DESC);

CREATE TABLE IF NOT EXISTS payments.fx_locks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID,
    source_currency VARCHAR(3) NOT NULL,
    target_currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    locked_rate DECIMAL(16,6) NOT NULL,
    source_amount BIGINT NOT NULL,
    target_amount_paise BIGINT NOT NULL,
    locked_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ NOT NULL,
    used BOOLEAN DEFAULT FALSE
);
CREATE INDEX idx_fx_locks_booking ON payments.fx_locks(booking_id);
