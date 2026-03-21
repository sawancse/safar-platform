-- Safar User Service Schema
-- V2: User profile extensions (name, email, avatar, language)

CREATE TABLE users.profiles (
    user_id     UUID PRIMARY KEY,
    name        VARCHAR(255) NOT NULL DEFAULT '',
    email       VARCHAR(255),
    avatar_url  TEXT,
    language    VARCHAR(10) NOT NULL DEFAULT 'en',
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_profiles_email ON users.profiles(email);
