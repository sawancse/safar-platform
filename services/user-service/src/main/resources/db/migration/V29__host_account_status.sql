-- V29: Host account suspension (Airbnb-style)
ALTER TABLE users.profiles ADD COLUMN IF NOT EXISTS account_status VARCHAR(20) DEFAULT 'ACTIVE';
ALTER TABLE users.profiles ADD COLUMN IF NOT EXISTS suspended_at TIMESTAMPTZ;
ALTER TABLE users.profiles ADD COLUMN IF NOT EXISTS suspension_reason TEXT;
ALTER TABLE users.profiles ADD COLUMN IF NOT EXISTS suspended_by UUID;

CREATE INDEX IF NOT EXISTS idx_profiles_account_status ON users.profiles(account_status);
