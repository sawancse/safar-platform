-- ── 1. Chef Calendar / Availability ──────────────────────────────────
CREATE TABLE chefs.chef_availability (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chef_id         UUID NOT NULL,
    blocked_date    DATE NOT NULL,
    reason          VARCHAR(255),
    created_at      TIMESTAMPTZ DEFAULT NOW()
);
CREATE UNIQUE INDEX idx_ca_chef_date ON chefs.chef_availability(chef_id, blocked_date);
CREATE INDEX idx_ca_chef ON chefs.chef_availability(chef_id);

-- ── 2. Chef Photo Gallery ───────────────────────────────────────────
CREATE TABLE chefs.chef_photos (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chef_id     UUID NOT NULL,
    url         TEXT NOT NULL,
    caption     VARCHAR(255),
    photo_type  VARCHAR(30) DEFAULT 'FOOD',
    sort_order  INTEGER DEFAULT 0,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX idx_cp_chef ON chefs.chef_photos(chef_id);

-- ── 3. Chef Badges ──────────────────────────────────────────────────
ALTER TABLE chefs.chef_profiles ADD COLUMN badge VARCHAR(30);
ALTER TABLE chefs.chef_profiles ADD COLUMN badge_awarded_at TIMESTAMPTZ;
ALTER TABLE chefs.chef_profiles ADD COLUMN referral_code VARCHAR(20) UNIQUE;
ALTER TABLE chefs.chef_profiles ADD COLUMN referred_by UUID;
ALTER TABLE chefs.chef_profiles ADD COLUMN referral_count INTEGER DEFAULT 0;
ALTER TABLE chefs.chef_profiles ADD COLUMN referral_earnings_paise BIGINT DEFAULT 0;

-- ── 4. Booking Scheduler Fields ─────────────────────────────────────
ALTER TABLE chefs.chef_bookings ADD COLUMN reminder_sent BOOLEAN DEFAULT FALSE;
ALTER TABLE chefs.chef_bookings ADD COLUMN auto_expire_at TIMESTAMPTZ;
ALTER TABLE chefs.chef_bookings ADD COLUMN eta_minutes INTEGER;
ALTER TABLE chefs.chef_bookings ADD COLUMN chef_lat DOUBLE PRECISION;
ALTER TABLE chefs.chef_bookings ADD COLUMN chef_lng DOUBLE PRECISION;
ALTER TABLE chefs.chef_bookings ADD COLUMN location_updated_at TIMESTAMPTZ;

ALTER TABLE chefs.event_bookings ADD COLUMN reminder_sent BOOLEAN DEFAULT FALSE;

-- ── 5. Invoice Tracking ─────────────────────────────────────────────
ALTER TABLE chefs.chef_bookings ADD COLUMN invoice_number VARCHAR(30);
ALTER TABLE chefs.event_bookings ADD COLUMN invoice_number VARCHAR(30);

-- ── 6. Cuisine Premium Pricing ──────────────────────────────────────
CREATE TABLE chefs.cuisine_price_tiers (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chef_id         UUID NOT NULL,
    cuisine_type    VARCHAR(30) NOT NULL,
    price_per_plate_paise BIGINT NOT NULL,
    created_at      TIMESTAMPTZ DEFAULT NOW()
);
CREATE UNIQUE INDEX idx_cpt_chef_cuisine ON chefs.cuisine_price_tiers(chef_id, cuisine_type);

-- ── 7. Chef Referral Tracking ───────────────────────────────────────
CREATE TABLE chefs.chef_referrals (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    referrer_id     UUID NOT NULL,
    referred_chef_id UUID NOT NULL,
    bonus_paise     BIGINT DEFAULT 50000,
    status          VARCHAR(20) DEFAULT 'PENDING',
    created_at      TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX idx_cr_referrer ON chefs.chef_referrals(referrer_id);
