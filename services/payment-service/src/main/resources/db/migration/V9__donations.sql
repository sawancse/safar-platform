-- Aashray donation tracking
CREATE TABLE IF NOT EXISTS payments.donations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    donation_ref    VARCHAR(30)  NOT NULL UNIQUE,
    donor_id        UUID,
    donor_name      VARCHAR(255),
    donor_email     VARCHAR(255),
    donor_phone     VARCHAR(20),
    donor_pan       VARCHAR(10),
    amount_paise    BIGINT       NOT NULL,
    currency        VARCHAR(3)   NOT NULL DEFAULT 'INR',
    frequency       VARCHAR(20)  NOT NULL DEFAULT 'ONE_TIME',
    razorpay_order_id       VARCHAR(100) UNIQUE,
    razorpay_payment_id     VARCHAR(100) UNIQUE,
    razorpay_subscription_id VARCHAR(100),
    payment_method  VARCHAR(30),
    status          VARCHAR(20)  NOT NULL DEFAULT 'CREATED',
    dedicated_to    VARCHAR(255),
    dedication_message TEXT,
    campaign_code   VARCHAR(50),
    receipt_number  VARCHAR(30)  UNIQUE,
    receipt_sent    BOOLEAN      NOT NULL DEFAULT FALSE,
    captured_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_donations_donor_id ON payments.donations(donor_id);
CREATE INDEX idx_donations_status ON payments.donations(status);
CREATE INDEX idx_donations_campaign ON payments.donations(campaign_code);
CREATE INDEX idx_donations_captured_at ON payments.donations(captured_at DESC);

-- Materialized stats (singleton row)
CREATE TABLE IF NOT EXISTS payments.donation_stats (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    total_raised_paise BIGINT  NOT NULL DEFAULT 0,
    goal_paise      BIGINT     NOT NULL DEFAULT 50000000,
    total_donors    INTEGER    NOT NULL DEFAULT 0,
    families_housed INTEGER    NOT NULL DEFAULT 0,
    monthly_donors  INTEGER    NOT NULL DEFAULT 0,
    active_campaign VARCHAR(100),
    campaign_tagline VARCHAR(255),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Seed initial stats row
INSERT INTO payments.donation_stats (id, total_raised_paise, goal_paise, total_donors, families_housed, monthly_donors)
VALUES (gen_random_uuid(), 0, 50000000, 0, 0, 0);
