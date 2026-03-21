CREATE TABLE listings.hospital_partners (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(200) NOT NULL,
    city            VARCHAR(100) NOT NULL,
    address         TEXT NOT NULL,
    lat             NUMERIC(9,6),
    lng             NUMERIC(9,6),
    specialties     TEXT NOT NULL DEFAULT '',
    accreditations  TEXT NOT NULL DEFAULT '',
    contact_email   VARCHAR(255),
    active          BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE listings.medical_stay_packages (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id      UUID NOT NULL,
    hospital_id     UUID NOT NULL,
    distance_km     NUMERIC(5,2),
    includes_pickup BOOLEAN NOT NULL DEFAULT false,
    includes_translator BOOLEAN NOT NULL DEFAULT false,
    caregiver_friendly  BOOLEAN NOT NULL DEFAULT false,
    medical_price_paise BIGINT NOT NULL,
    min_stay_nights INT NOT NULL DEFAULT 3,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE listings.listings ADD COLUMN IF NOT EXISTS medical_stay BOOLEAN DEFAULT false;
