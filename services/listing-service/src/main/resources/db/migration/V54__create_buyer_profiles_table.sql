-- V54: Buyer Profiles for property search preferences
CREATE TABLE buyer_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE,
    preferred_cities TEXT[],
    preferred_localities TEXT[],
    budget_min_paise BIGINT,
    budget_max_paise BIGINT,
    preferred_bhk TEXT[],
    preferred_types TEXT[],
    financing_type VARCHAR(15),
    possession_timeline VARCHAR(20) DEFAULT 'FLEXIBLE',
    alerts_enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_buyer_profile_user ON buyer_profiles(user_id);
