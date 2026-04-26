-- V25: Commission tier on service listings (Hybrid C monetization model).
--
-- Flexible-by-design: rates and thresholds are stored in a DB table
-- (commission_rate_config), seeded with sensible defaults but admin-editable
-- without code changes. Individual vendors can carry a per-vendor override
-- (commission_pct_override) for negotiated deals. Lookup precedence:
--
--    vendor.commission_pct_override
--      → commission_rate_config[service_type, current_tier]
--      → hardcoded fallback (defensive)
--
-- Tier names are universal (STARTER → PRO → COMMERCIAL) but rates and
-- promotion thresholds vary per service_type so a high-margin cake baker
-- and a thin-margin staff agency at the same tier pay different rates.

-- ── Listing-level columns ───────────────────────────────────────────

ALTER TABLE services.service_listings
    ADD COLUMN IF NOT EXISTS commission_tier         VARCHAR(20)   NOT NULL DEFAULT 'STARTER',
    ADD COLUMN IF NOT EXISTS subscription_plan       VARCHAR(20)   NOT NULL DEFAULT 'NONE',
    ADD COLUMN IF NOT EXISTS subscription_expires_at TIMESTAMPTZ,
    -- Per-vendor commission override — NULL means "use config table".
    ADD COLUMN IF NOT EXISTS commission_pct_override NUMERIC(5,2),
    ADD COLUMN IF NOT EXISTS commission_override_reason TEXT;

ALTER TABLE services.service_listings
    ADD CONSTRAINT chk_service_listings_commission_tier
        CHECK (commission_tier IN ('STARTER', 'PRO', 'COMMERCIAL'));

ALTER TABLE services.service_listings
    ADD CONSTRAINT chk_service_listings_subscription_plan
        CHECK (subscription_plan IN ('NONE', 'FEATURED', 'PRO_VENDOR'));

ALTER TABLE services.service_listings
    ADD CONSTRAINT chk_service_listings_commission_override_range
        CHECK (commission_pct_override IS NULL OR (commission_pct_override >= 0 AND commission_pct_override <= 50));

CREATE INDEX IF NOT EXISTS idx_listings_commission_tier
    ON services.service_listings(commission_tier) WHERE status = 'VERIFIED';

-- ── Platform commission rate config (admin-editable) ────────────────

CREATE TABLE IF NOT EXISTS services.commission_rate_config (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_type        VARCHAR(40) NOT NULL,
    tier                VARCHAR(20) NOT NULL,
    commission_pct      NUMERIC(5,2) NOT NULL,
    promotion_threshold INT NOT NULL DEFAULT 0,    -- completed_bookings_count needed to enter this tier
    notes               TEXT,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by          UUID,

    CONSTRAINT uq_commission_rate_type_tier UNIQUE (service_type, tier),
    CONSTRAINT chk_commission_rate_tier CHECK (tier IN ('STARTER','PRO','COMMERCIAL')),
    CONSTRAINT chk_commission_rate_pct  CHECK (commission_pct >= 0 AND commission_pct <= 50),
    CONSTRAINT chk_commission_rate_thr  CHECK (promotion_threshold >= 0)
);

CREATE INDEX IF NOT EXISTS idx_commission_rate_lookup
    ON services.commission_rate_config(service_type, tier);

-- ── Seed defaults ───────────────────────────────────────────────────
-- Spread rationale:
--   * High-margin verticals → tighter take (cake/pandit/appliance)
--   * Mid-margin → legacy default rate (singer/cook)
--   * High-ticket heavy material/labour → mid take (decor)
--   * Thin-margin operational → lowest take (staff_hire)
--   * Pandit gets easier promotion thresholds (5/25 vs 10/50) — low volume vertical

INSERT INTO services.commission_rate_config (service_type, tier, commission_pct, promotion_threshold, notes)
VALUES
  -- High-margin
  ('CAKE_DESIGNER',    'STARTER',    15.00,  0, 'High gross margin — tighter platform take'),
  ('CAKE_DESIGNER',    'PRO',        10.00, 10, NULL),
  ('CAKE_DESIGNER',    'COMMERCIAL',  8.00, 50, NULL),

  ('PANDIT',           'STARTER',    15.00,  0, 'Low-volume vertical — easier promotion thresholds'),
  ('PANDIT',           'PRO',        12.00,  5, NULL),
  ('PANDIT',           'COMMERCIAL', 10.00, 25, NULL),

  ('APPLIANCE_RENTAL', 'STARTER',    15.00,  0, NULL),
  ('APPLIANCE_RENTAL', 'PRO',        12.00, 10, NULL),
  ('APPLIANCE_RENTAL', 'COMMERCIAL', 10.00, 50, NULL),

  -- Mid-margin (legacy defaults)
  ('SINGER',           'STARTER',    18.00,  0, NULL),
  ('SINGER',           'PRO',        12.00, 10, NULL),
  ('SINGER',           'COMMERCIAL', 10.00, 50, NULL),

  ('COOK',             'STARTER',    18.00,  0, 'Matches legacy chef commission'),
  ('COOK',             'PRO',        12.00, 10, NULL),
  ('COOK',             'COMMERCIAL', 10.00, 50, NULL),

  -- High-ticket, heavy material/labour share
  ('DECORATOR',        'STARTER',    12.00,  0, 'High ticket — heavy material cost share'),
  ('DECORATOR',        'PRO',        10.00, 10, NULL),
  ('DECORATOR',        'COMMERCIAL',  8.00, 50, NULL),

  -- Thin-margin operational
  ('STAFF_HIRE',       'STARTER',    10.00,  0, 'Thin margin — mostly labour cost'),
  ('STAFF_HIRE',       'PRO',         8.00, 10, NULL),
  ('STAFF_HIRE',       'COMMERCIAL',  6.00, 50, NULL),

  -- V2 types — legacy defaults until reviewed
  ('PHOTOGRAPHER',     'STARTER',    18.00,  0, NULL),
  ('PHOTOGRAPHER',     'PRO',        12.00, 10, NULL),
  ('PHOTOGRAPHER',     'COMMERCIAL', 10.00, 50, NULL),

  ('DJ',               'STARTER',    18.00,  0, NULL),
  ('DJ',               'PRO',        12.00, 10, NULL),
  ('DJ',               'COMMERCIAL', 10.00, 50, NULL),

  ('MEHENDI',          'STARTER',    18.00,  0, NULL),
  ('MEHENDI',          'PRO',        12.00, 10, NULL),
  ('MEHENDI',          'COMMERCIAL', 10.00, 50, NULL),

  ('MAKEUP_ARTIST',    'STARTER',    18.00,  0, NULL),
  ('MAKEUP_ARTIST',    'PRO',        12.00, 10, NULL),
  ('MAKEUP_ARTIST',    'COMMERCIAL', 10.00, 50, NULL)
ON CONFLICT (service_type, tier) DO NOTHING;
