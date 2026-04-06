-- V24: Referral system for guest and host referrals
CREATE TABLE users.referrals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    referrer_id UUID NOT NULL,
    referred_id UUID,
    referral_code VARCHAR(12) NOT NULL UNIQUE,
    type VARCHAR(10) NOT NULL DEFAULT 'GUEST',
    status VARCHAR(15) NOT NULL DEFAULT 'PENDING',
    referrer_reward_paise BIGINT DEFAULT 50000,
    referred_reward_paise BIGINT DEFAULT 25000,
    referrer_credited BOOLEAN DEFAULT FALSE,
    referred_credited BOOLEAN DEFAULT FALSE,
    qualifying_booking_id UUID,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    completed_at TIMESTAMPTZ
);

CREATE INDEX idx_referrals_referrer ON users.referrals(referrer_id);
CREATE INDEX idx_referrals_referred ON users.referrals(referred_id);
CREATE INDEX idx_referrals_code ON users.referrals(referral_code);
CREATE INDEX idx_referrals_status ON users.referrals(status);

-- Add referral_code to user_profiles for quick lookup
ALTER TABLE users.profiles ADD COLUMN IF NOT EXISTS referral_code VARCHAR(12) UNIQUE;
ALTER TABLE users.profiles ADD COLUMN IF NOT EXISTS referred_by_code VARCHAR(12);
ALTER TABLE users.profiles ADD COLUMN IF NOT EXISTS total_referrals INT DEFAULT 0;
ALTER TABLE users.profiles ADD COLUMN IF NOT EXISTS referral_earnings_paise BIGINT DEFAULT 0;
