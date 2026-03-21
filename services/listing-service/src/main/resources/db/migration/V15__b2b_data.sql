CREATE TABLE listings.b2b_api_subscriptions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_name    VARCHAR(200) NOT NULL,
    contact_email   VARCHAR(255) NOT NULL,
    api_key         VARCHAR(64) NOT NULL UNIQUE,
    plan            VARCHAR(20) NOT NULL DEFAULT 'STARTER',
    monthly_calls   BIGINT NOT NULL DEFAULT 0,
    call_limit      BIGINT NOT NULL DEFAULT 10000,
    price_monthly_paise BIGINT NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_b2b_api_key ON listings.b2b_api_subscriptions(api_key);
