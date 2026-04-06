-- User login PIN (HDFC-style quick login, replaces OTP)
ALTER TABLE auth.users ADD COLUMN IF NOT EXISTS pin_hash VARCHAR(100);
ALTER TABLE auth.users ADD COLUMN IF NOT EXISTS pin_set_at TIMESTAMPTZ;
ALTER TABLE auth.users ADD COLUMN IF NOT EXISTS pin_failed_attempts INTEGER NOT NULL DEFAULT 0;
ALTER TABLE auth.users ADD COLUMN IF NOT EXISTS pin_locked_until TIMESTAMPTZ;
