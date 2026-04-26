-- User flags + push tokens.
--
-- Flags drive the cross-vertical Trip Intent engine — booking-service's
-- TripIntentEvaluator queries this to know if MEDICAL/HISTORY rules
-- should fire. Stored as a JSONB string array for flexibility (no need
-- for a typed enum; flags are added by other services as they identify
-- user states like medical_history, new_pg_signup, etc.).
--
-- Push tokens enable the Expo push channel — notification-service's
-- PushNotificationService queries this to deliver push reminders.

-- Flags as a JSONB column on profiles (one row per user already)
ALTER TABLE users.profiles
    ADD COLUMN IF NOT EXISTS user_flags JSONB NOT NULL DEFAULT '[]'::jsonb;

CREATE INDEX IF NOT EXISTS idx_profiles_user_flags
    ON users.profiles USING gin (user_flags);

-- Push tokens — one row per (user, device) so users can have multiple
-- devices (phone + tablet etc.).
CREATE TABLE IF NOT EXISTS users.user_push_tokens (
    id            UUID         PRIMARY KEY,
    user_id       UUID         NOT NULL,
    push_token    VARCHAR(200) NOT NULL,            -- Expo: 'ExponentPushToken[xxx]' (~80-150 chars)
    platform      VARCHAR(10)  NOT NULL,            -- 'ios' / 'android' / 'web'
    device_id     VARCHAR(100),                      -- mobile device id; lets one user revoke a single device
    last_used_at  TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_push_tokens_user_id ON users.user_push_tokens (user_id);
CREATE UNIQUE INDEX IF NOT EXISTS uniq_push_tokens_user_token
    ON users.user_push_tokens (user_id, push_token);
