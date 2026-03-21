-- Safar User Service Schema
-- V1: Initial schema creation

CREATE SCHEMA IF NOT EXISTS users;

CREATE TABLE users.taste_profiles (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID UNIQUE NOT NULL,
    travel_style    VARCHAR(20) CHECK (travel_style IN ('ADVENTURE','RELAXED','CULTURAL','WELLNESS')),
    property_vibe   VARCHAR(20) CHECK (property_vibe IN ('COZY','MODERN','RUSTIC','LUXURY')),
    must_haves      TEXT[],
    group_type      VARCHAR(20) CHECK (group_type IN ('SOLO','COUPLE','FAMILY','FRIENDS','BUSINESS')),
    budget_tier     VARCHAR(20) CHECK (budget_tier IN ('BUDGET','MID','PREMIUM','LUXURY')),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_taste_profiles_user ON users.taste_profiles(user_id);

CREATE TABLE users.host_subscriptions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    host_id             UUID UNIQUE NOT NULL,
    tier                VARCHAR(20) NOT NULL
                            CHECK (tier IN ('STARTER','PRO','COMMERCIAL')),
    status              VARCHAR(20) NOT NULL DEFAULT 'TRIAL'
                            CHECK (status IN ('TRIAL','ACTIVE','PAUSED','CANCELLED')),
    trial_ends_at       TIMESTAMPTZ,
    billing_cycle       VARCHAR(10) NOT NULL DEFAULT 'MONTHLY',
    razorpay_sub_id     VARCHAR(100),
    amount_paise        INTEGER NOT NULL,
    next_billing_at     TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_host_subs_host_id ON users.host_subscriptions(host_id);
CREATE INDEX idx_host_subs_status  ON users.host_subscriptions(status);
