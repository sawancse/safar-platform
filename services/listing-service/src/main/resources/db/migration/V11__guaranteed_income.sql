CREATE TABLE listings.guaranteed_income_contracts (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id              UUID NOT NULL,
    host_id                 UUID NOT NULL,
    monthly_guarantee_paise BIGINT NOT NULL,
    contract_start          DATE NOT NULL,
    contract_end            DATE NOT NULL,
    status                  VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    total_paid_out_paise    BIGINT NOT NULL DEFAULT 0,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE listings.guaranteed_income_settlements (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contract_id         UUID NOT NULL,
    period_month        DATE NOT NULL,
    actual_revenue      BIGINT NOT NULL,
    guarantee_amount    BIGINT NOT NULL,
    shortfall           BIGINT NOT NULL DEFAULT 0,
    safar_topped_up     BIGINT NOT NULL DEFAULT 0,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    settled_at          TIMESTAMPTZ
);
