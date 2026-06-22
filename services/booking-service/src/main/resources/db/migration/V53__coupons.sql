-- V53: Coupon / promo-code system (MakeMyTrip style). Supports PLATFORM-wide
-- admin coupons AND HOST per-listing coupons (scope + owner_id + listing_id).

CREATE TABLE IF NOT EXISTS bookings.coupons (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code                VARCHAR(40)  NOT NULL,
    scope               VARCHAR(20)  NOT NULL DEFAULT 'PLATFORM',   -- PLATFORM | HOST
    owner_id            UUID,                                       -- host user id for HOST scope (null for PLATFORM)
    listing_id          UUID,                                       -- optional: restrict to a single listing
    discount_type       VARCHAR(20)  NOT NULL,                      -- PERCENT | FLAT
    percent_off         INT,                                        -- when PERCENT
    max_discount_paise  BIGINT,                                     -- cap for PERCENT (null = uncapped)
    flat_off_paise      BIGINT,                                     -- when FLAT
    min_booking_paise   BIGINT       NOT NULL DEFAULT 0,
    valid_from          DATE,
    valid_until         DATE,
    usage_limit         INT,                                        -- total redemptions allowed (null = unlimited)
    used_count          INT          NOT NULL DEFAULT 0,
    per_user_limit      INT,                                        -- per-user cap (null = unlimited)
    active              BOOLEAN      NOT NULL DEFAULT TRUE,
    description         TEXT,
    created_by          UUID,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_coupons_code  UNIQUE (code),
    CONSTRAINT chk_coupons_scope CHECK (scope IN ('PLATFORM','HOST')),
    CONSTRAINT chk_coupons_type  CHECK (discount_type IN ('PERCENT','FLAT'))
);

CREATE INDEX IF NOT EXISTS idx_coupons_code_active ON bookings.coupons(code) WHERE active;
CREATE INDEX IF NOT EXISTS idx_coupons_owner       ON bookings.coupons(owner_id);

CREATE TABLE IF NOT EXISTS bookings.coupon_redemptions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    coupon_id       UUID         NOT NULL REFERENCES bookings.coupons(id) ON DELETE CASCADE,
    user_id         UUID         NOT NULL,
    booking_id      UUID,
    discount_paise  BIGINT       NOT NULL,
    redeemed_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_coupon_redemptions_coupon_user ON bookings.coupon_redemptions(coupon_id, user_id);

-- Booking carries the applied coupon for audit + redemption at confirm.
ALTER TABLE bookings.bookings
    ADD COLUMN IF NOT EXISTS coupon_code          VARCHAR(40),
    ADD COLUMN IF NOT EXISTS coupon_discount_paise BIGINT DEFAULT 0;
