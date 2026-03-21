CREATE TABLE listings.rw_certifications (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id          UUID NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    wifi_speed_mbps     INT,
    has_dedicated_desk  BOOLEAN DEFAULT false,
    has_power_backup    BOOLEAN DEFAULT false,
    quiet_hours_from    TIME,
    quiet_hours_to      TIME,
    additional_notes    TEXT,
    certified_at        TIMESTAMPTZ,
    expires_at          TIMESTAMPTZ,
    submitted_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    admin_note          TEXT
);

ALTER TABLE listings.listings ADD COLUMN IF NOT EXISTS rw_certified BOOLEAN DEFAULT false;
ALTER TABLE listings.listings ADD COLUMN IF NOT EXISTS rw_certified_at TIMESTAMPTZ;
