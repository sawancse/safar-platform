-- V16: Aashray organizations for refugee housing
CREATE TABLE IF NOT EXISTS users.organizations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(200) NOT NULL,
    type VARCHAR(50) NOT NULL DEFAULT 'NGO',
    unhcr_partner_code VARCHAR(50),
    contact_email VARCHAR(255),
    contact_phone VARCHAR(20),
    budget_paise BIGINT NOT NULL DEFAULT 0,
    spent_paise BIGINT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS users.case_workers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    organization_id UUID NOT NULL REFERENCES users.organizations(id),
    role VARCHAR(50) NOT NULL DEFAULT 'CASE_WORKER',
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_case_workers_org ON users.case_workers (organization_id);
CREATE INDEX IF NOT EXISTS idx_case_workers_user ON users.case_workers (user_id);
