-- Talk-to-advisor / assisted-sales leads (PolicyBazaar tele-advisor model)
CREATE TABLE IF NOT EXISTS insurance.insurance_leads (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID,
    name            VARCHAR(150) NOT NULL,
    phone           VARCHAR(20)  NOT NULL,
    email           VARCHAR(200),
    product         VARCHAR(40),                 -- health / term-life / motor / travel / ...
    coverage_type   VARCHAR(30),
    city            VARCHAR(120),
    preferred_time  VARCHAR(60),                 -- e.g. "Today 6-8 PM"
    notes           VARCHAR(1000),
    status          VARCHAR(20) NOT NULL DEFAULT 'NEW',  -- NEW / CONTACTED / CONVERTED / LOST
    source          VARCHAR(40) NOT NULL DEFAULT 'WEB_ADVISOR',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_insurance_leads_status  ON insurance.insurance_leads (status);
CREATE INDEX IF NOT EXISTS idx_insurance_leads_created ON insurance.insurance_leads (created_at DESC);
