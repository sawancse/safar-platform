-- Email lead capture for marketing campaigns
CREATE TABLE IF NOT EXISTS users.user_leads (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL,
    name VARCHAR(200),
    phone VARCHAR(20),
    city VARCHAR(100),
    source VARCHAR(50) NOT NULL DEFAULT 'WEBSITE_POPUP',
    utm_source VARCHAR(100),
    utm_medium VARCHAR(100),
    utm_campaign VARCHAR(100),
    subscribed BOOLEAN DEFAULT TRUE,
    converted BOOLEAN DEFAULT FALSE,
    converted_user_id UUID,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

CREATE UNIQUE INDEX idx_user_leads_email ON users.user_leads(email);
CREATE INDEX idx_user_leads_city ON users.user_leads(city);
CREATE INDEX idx_user_leads_created ON users.user_leads(created_at DESC);

COMMENT ON TABLE users.user_leads IS 'Pre-signup email leads from website popups, landing pages, etc.';
COMMENT ON COLUMN users.user_leads.source IS 'WEBSITE_POPUP, LANDING_PAGE, EXIT_INTENT, PRICE_ALERT, REFERRAL';
