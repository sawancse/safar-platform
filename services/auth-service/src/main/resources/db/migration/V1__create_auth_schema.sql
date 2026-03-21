-- Safar Auth Service Schema
-- V1: Initial schema creation

CREATE SCHEMA IF NOT EXISTS auth;

CREATE TABLE auth.users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone           VARCHAR(13) UNIQUE NOT NULL,
    email           VARCHAR(255) UNIQUE,
    name            VARCHAR(255) NOT NULL,
    role            VARCHAR(20) NOT NULL DEFAULT 'GUEST'
                        CHECK (role IN ('HOST','GUEST','BOTH','ADMIN')),
    avatar_url      TEXT,
    kyc_status      VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                        CHECK (kyc_status IN ('PENDING','VERIFIED','FAILED')),
    aadhaar_ref     VARCHAR(50),
    pan_ref         VARCHAR(10),
    language        VARCHAR(10) NOT NULL DEFAULT 'en',
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_auth_users_phone ON auth.users(phone);
CREATE INDEX idx_auth_users_email ON auth.users(email);
CREATE INDEX idx_auth_users_role  ON auth.users(role);
