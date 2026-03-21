-- S18: Guest Traveler Pro subscription
CREATE TABLE users.guest_subscriptions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    guest_id        UUID UNIQUE NOT NULL,
    razorpay_sub_id VARCHAR(100),
    status          VARCHAR(20) DEFAULT 'ACTIVE'
                        CHECK (status IN ('ACTIVE','PAUSED','CANCELLED')),
    trial_ends_at   TIMESTAMPTZ,
    next_billing_at TIMESTAMPTZ,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

-- S20: Guest wallet (social share credits)
CREATE TABLE users.guest_wallets (
    id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    guest_id                  UUID UNIQUE NOT NULL,
    balance_paise             BIGINT DEFAULT 0,
    lifetime_earned_paise     BIGINT DEFAULT 0,
    created_at                TIMESTAMPTZ DEFAULT NOW(),
    updated_at                TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE users.social_shares (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    guest_id        UUID NOT NULL,
    booking_id      UUID NOT NULL,
    platform        VARCHAR(20) CHECK (platform IN ('INSTAGRAM','TWITTER','FACEBOOK','LINKEDIN')),
    share_proof_url TEXT,
    credits_paise   BIGINT DEFAULT 29900,
    status          VARCHAR(20) DEFAULT 'PENDING'
                        CHECK (status IN ('PENDING','VERIFIED','REJECTED')),
    verified_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_social_shares_guest ON users.social_shares(guest_id);
