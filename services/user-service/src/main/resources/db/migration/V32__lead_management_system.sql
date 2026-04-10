-- Lead Management System: scoring, alerts, nurturing, segments

-- Enhance user_leads with scoring + nurturing fields
ALTER TABLE users.user_leads
    ADD COLUMN IF NOT EXISTS lead_score INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS intent_score INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS behavioral_score INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS demographic_score INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS recency_score INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS segment VARCHAR(30) DEFAULT 'NEW',
    ADD COLUMN IF NOT EXISTS whatsapp_optin BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS last_active_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS pages_viewed INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS searches_performed INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS listings_viewed INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS wishlist_count INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS checkout_attempted BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS last_search_query VARCHAR(500),
    ADD COLUMN IF NOT EXISTS last_search_city VARCHAR(100),
    ADD COLUMN IF NOT EXISTS nurture_stage VARCHAR(30) DEFAULT 'NONE',
    ADD COLUMN IF NOT EXISTS nurture_day0_sent BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS nurture_day3_sent BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS nurture_day7_sent BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS converted_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS lead_type VARCHAR(20) DEFAULT 'GUEST';

CREATE INDEX IF NOT EXISTS idx_leads_score ON users.user_leads(lead_score DESC);
CREATE INDEX IF NOT EXISTS idx_leads_segment ON users.user_leads(segment);
CREATE INDEX IF NOT EXISTS idx_leads_nurture ON users.user_leads(nurture_stage);
CREATE INDEX IF NOT EXISTS idx_leads_last_active ON users.user_leads(last_active_at DESC);

-- Price alerts: notify when listing price drops below threshold
CREATE TABLE IF NOT EXISTS users.price_alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL,
    user_id UUID,
    listing_id UUID NOT NULL,
    listing_title VARCHAR(500),
    listing_city VARCHAR(100),
    threshold_paise BIGINT NOT NULL,
    current_price_paise BIGINT,
    active BOOLEAN DEFAULT TRUE,
    triggered_count INTEGER DEFAULT 0,
    last_triggered_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_price_alerts_listing ON users.price_alerts(listing_id);
CREATE INDEX IF NOT EXISTS idx_price_alerts_email ON users.price_alerts(email);
CREATE INDEX IF NOT EXISTS idx_price_alerts_active ON users.price_alerts(active) WHERE active = TRUE;

-- Locality alerts: notify when new listing in area
CREATE TABLE IF NOT EXISTS users.locality_alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL,
    user_id UUID,
    city VARCHAR(100) NOT NULL,
    locality VARCHAR(200),
    listing_type VARCHAR(50),
    max_price_paise BIGINT,
    active BOOLEAN DEFAULT TRUE,
    triggered_count INTEGER DEFAULT 0,
    last_triggered_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_locality_alerts_city ON users.locality_alerts(city);
CREATE INDEX IF NOT EXISTS idx_locality_alerts_active ON users.locality_alerts(active) WHERE active = TRUE;

-- Nurture campaigns: track email sequences
CREATE TABLE IF NOT EXISTS users.nurture_campaigns (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(200) NOT NULL,
    campaign_type VARCHAR(50) NOT NULL, -- WELCOME_DRIP, ABANDONED_SEARCH, FESTIVAL, PRICE_DROP, HOST_ACQUISITION, RE_ENGAGEMENT
    target_segment VARCHAR(50),
    subject_template VARCHAR(500),
    email_template VARCHAR(200),
    delay_hours INTEGER DEFAULT 0,
    active BOOLEAN DEFAULT TRUE,
    sent_count INTEGER DEFAULT 0,
    open_count INTEGER DEFAULT 0,
    click_count INTEGER DEFAULT 0,
    conversion_count INTEGER DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Seed default nurture campaigns
INSERT INTO users.nurture_campaigns (name, campaign_type, target_segment, subject_template, email_template, delay_hours) VALUES
    ('Welcome — Instant', 'WELCOME_DRIP', 'NEW', 'Welcome to Safar! Your journey begins here', 'lead-welcome', 0),
    ('Welcome — Day 3', 'WELCOME_DRIP', 'NEW', 'Top stays in {{city}} waiting for you', 'lead-day3-deals', 72),
    ('Welcome — Day 7 Offer', 'WELCOME_DRIP', 'NEW', '₹500 off your first stay — expires in 48 hours!', 'lead-day7-offer', 168),
    ('Abandoned Search', 'ABANDONED_SEARCH', 'WARM', 'Still looking for stays in {{city}}?', 'lead-abandoned-search', 24),
    ('Price Drop Alert', 'PRICE_DROP', 'ALL', 'Price dropped on {{listingTitle}}!', 'lead-price-drop', 0),
    ('Festival — Diwali', 'FESTIVAL', 'ALL', 'Diwali getaway deals — book before they go!', 'lead-festival', 0),
    ('Festival — Holi', 'FESTIVAL', 'ALL', 'Holi special — colour your vacation!', 'lead-festival', 0),
    ('Host Acquisition', 'HOST_ACQUISITION', 'HOST_PROSPECT', 'Your property could earn ₹45,000/month on Safar', 'lead-host-earn', 0),
    ('Re-engagement — 30d', 'RE_ENGAGEMENT', 'COLD', 'We miss you! New stays added in {{city}}', 'lead-re-engagement', 720)
ON CONFLICT DO NOTHING;

-- Lead activity log: track every interaction for scoring
CREATE TABLE IF NOT EXISTS users.lead_activities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lead_id UUID REFERENCES users.user_leads(id),
    email VARCHAR(255),
    activity_type VARCHAR(50) NOT NULL, -- PAGE_VIEW, SEARCH, LISTING_VIEW, WISHLIST, CHECKOUT_START, EMAIL_OPEN, EMAIL_CLICK
    metadata JSONB,
    score_delta INTEGER DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_lead_activities_lead ON users.lead_activities(lead_id);
CREATE INDEX IF NOT EXISTS idx_lead_activities_email ON users.lead_activities(email);
CREATE INDEX IF NOT EXISTS idx_lead_activities_type ON users.lead_activities(activity_type);
