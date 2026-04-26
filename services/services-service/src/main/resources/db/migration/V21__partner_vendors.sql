-- V21: Partner Vendor directory — admin-onboarded providers for the
-- bespoke event services (cake, decor, pandit, live-music, appliances,
-- staff-hire). One generic table covers all 6 service_type values via
-- the service_type discriminator. Pricing overrides + portfolio are
-- stored as JSONB so each service can carry its own shape without
-- schema churn.
--
-- Onboarding is admin-only at this phase: vendors do not self-serve,
-- payouts are manual NEFT (admin enters UTR after transfer).

CREATE TABLE IF NOT EXISTS chefs.partner_vendors (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_type          VARCHAR(30)  NOT NULL,  -- CAKE_DESIGNER / EVENT_DECOR / PANDIT_PUJA / LIVE_MUSIC / APPLIANCE_RENTAL / STAFF_HIRE
    business_name         VARCHAR(160) NOT NULL,
    owner_name            VARCHAR(120),
    phone                 VARCHAR(20)  NOT NULL,
    email                 VARCHAR(160),
    whatsapp              VARCHAR(20),            -- falls back to phone if null
    gst                   VARCHAR(20),
    pan                   VARCHAR(15),
    bank_account          VARCHAR(40),
    bank_ifsc             VARCHAR(15),
    bank_holder           VARCHAR(120),
    address               TEXT,
    service_cities        TEXT[]       DEFAULT '{}',  -- lowercase city names; empty = serves anywhere
    service_radius_km     INT          DEFAULT 25,
    portfolio_json        JSONB,                  -- { photos: [], specialities: [], samples: [] }
    pricing_override_json JSONB,                  -- per-vendor price overrides; NULL = use platform defaults
    kyc_status            VARCHAR(20)  DEFAULT 'PENDING',  -- PENDING / VERIFIED / REJECTED
    kyc_notes             TEXT,
    rating_avg            NUMERIC(3,2),
    rating_count          INT          DEFAULT 0,
    jobs_completed        INT          DEFAULT 0,
    active                BOOLEAN      DEFAULT TRUE,
    notes                 TEXT,                   -- admin-only freeform
    created_at            TIMESTAMPTZ  DEFAULT now(),
    updated_at            TIMESTAMPTZ  DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_partner_vendors_type_active
  ON chefs.partner_vendors (service_type, active);

CREATE INDEX IF NOT EXISTS idx_partner_vendors_type_kyc
  ON chefs.partner_vendors (service_type, kyc_status);

CREATE INDEX IF NOT EXISTS idx_partner_vendors_cities
  ON chefs.partner_vendors USING GIN (service_cities);
