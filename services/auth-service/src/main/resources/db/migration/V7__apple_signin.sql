-- Add Apple Sign-In support
ALTER TABLE auth.users ADD COLUMN IF NOT EXISTS apple_id VARCHAR(255) UNIQUE;
CREATE INDEX IF NOT EXISTS idx_users_apple_id ON auth.users(apple_id);
