-- S21: AI property scout leads
ALTER TABLE listings.listings
    ADD COLUMN IF NOT EXISTS source VARCHAR(30) DEFAULT 'HOST'
        CHECK (source IN ('HOST','AGENCY_FEED','AI_SCOUT'));

CREATE TABLE listings.scout_leads (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    address                 TEXT NOT NULL,
    city                    VARCHAR(100) NOT NULL,
    lat                     DECIMAL(9,6),
    lng                     DECIMAL(9,6),
    estimated_income_paise  BIGINT,
    outreach_sent_at        TIMESTAMPTZ,
    status                  VARCHAR(20) DEFAULT 'PENDING'
                                CHECK (status IN ('PENDING','CONTACTED','CONVERTED')),
    created_at              TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_scout_leads_city ON listings.scout_leads(city);

-- S22: Pre-built listing drafts (AI-generated)
CREATE TABLE listings.listing_drafts (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    host_id                  UUID NOT NULL,
    address                  TEXT,
    type                     VARCHAR(20),
    ai_title                 TEXT,
    ai_description           TEXT,
    ai_amenities             TEXT,
    ai_suggested_price_paise BIGINT,
    status                   VARCHAR(20) DEFAULT 'DRAFT'
                                 CHECK (status IN ('DRAFT','APPROVED','REJECTED')),
    created_at               TIMESTAMPTZ DEFAULT NOW()
);
