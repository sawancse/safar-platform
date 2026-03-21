-- Reverse Search: "Looking For" profiles for PG/rental seekers
CREATE TABLE IF NOT EXISTS listings.seeker_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    seeker_type VARCHAR(30) NOT NULL DEFAULT 'PG_SEEKER',
    preferred_city VARCHAR(100) NOT NULL,
    preferred_locality VARCHAR(255),
    preferred_lat DOUBLE PRECISION,
    preferred_lng DOUBLE PRECISION,
    radius_km INT NOT NULL DEFAULT 5,
    budget_min_paise BIGINT NOT NULL DEFAULT 0,
    budget_max_paise BIGINT NOT NULL DEFAULT 0,
    preferred_sharing VARCHAR(30),
    gender_preference VARCHAR(20),
    preferred_amenities VARCHAR(500),
    move_in_date DATE,
    vegetarian_only BOOLEAN NOT NULL DEFAULT FALSE,
    pet_owner BOOLEAN NOT NULL DEFAULT FALSE,
    occupation VARCHAR(50),
    company_or_college VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    match_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sp_city ON listings.seeker_profiles(preferred_city);
CREATE INDEX idx_sp_status ON listings.seeker_profiles(status);
CREATE INDEX idx_sp_type ON listings.seeker_profiles(seeker_type);
CREATE INDEX idx_sp_gender ON listings.seeker_profiles(gender_preference);
CREATE INDEX idx_sp_user ON listings.seeker_profiles(user_id);
