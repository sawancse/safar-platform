-- Feature 4: Safar Star Host (earned badge like Airbnb Superhost)
-- Criteria: avg rating >= 4.8, cancel rate < 2%, 10+ completed stays, response rate >= 90%

ALTER TABLE users.profiles ADD COLUMN IF NOT EXISTS star_host BOOLEAN DEFAULT FALSE;
ALTER TABLE users.profiles ADD COLUMN IF NOT EXISTS star_host_since TIMESTAMP;
ALTER TABLE users.profiles ADD COLUMN IF NOT EXISTS avg_host_rating DOUBLE PRECISION;
ALTER TABLE users.profiles ADD COLUMN IF NOT EXISTS cancellation_rate_percent DOUBLE PRECISION DEFAULT 0;
ALTER TABLE users.profiles ADD COLUMN IF NOT EXISTS total_completed_stays INT DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_profiles_star_host ON users.profiles(star_host);
