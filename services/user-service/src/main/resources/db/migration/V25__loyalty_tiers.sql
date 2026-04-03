-- V25: Traveler loyalty tiers (Booking.com Genius-style)
ALTER TABLE users.user_profiles ADD COLUMN IF NOT EXISTS loyalty_tier VARCHAR(10) DEFAULT 'BRONZE';
ALTER TABLE users.user_profiles ADD COLUMN IF NOT EXISTS completed_stays INT DEFAULT 0;
ALTER TABLE users.user_profiles ADD COLUMN IF NOT EXISTS loyalty_points BIGINT DEFAULT 0;
ALTER TABLE users.user_profiles ADD COLUMN IF NOT EXISTS tier_upgraded_at TIMESTAMPTZ;

-- Loyalty transaction log
CREATE TABLE users.loyalty_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    points BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    description VARCHAR(200),
    booking_id UUID,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_loyalty_tx_user ON users.loyalty_transactions(user_id);
CREATE INDEX idx_loyalty_tx_type ON users.loyalty_transactions(type);
