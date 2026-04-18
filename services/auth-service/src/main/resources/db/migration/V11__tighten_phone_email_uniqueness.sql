-- V11: enforce phone + email uniqueness properly on auth.users
--
-- Before: phone VARCHAR(13) UNIQUE (treats NULLs as distinct — OK)
--         email VARCHAR(255) UNIQUE (case-sensitive — "Foo@x" and "foo@x" both pass)
--
-- After:  phone partial unique index on NOT NULL rows (equivalent behaviour, explicit)
--         email partial unique index on LOWER(email) where NOT NULL (case-insensitive)
--
-- Fails fast on existing duplicates so they are flagged rather than silently
-- swallowed. Admins clear dupes via POST /api/v1/admin/guests/merge before
-- re-running this migration.

-- Phone: replace the simple UNIQUE constraint with an explicit partial unique index
ALTER TABLE auth.users DROP CONSTRAINT IF EXISTS users_phone_key;
CREATE UNIQUE INDEX IF NOT EXISTS users_phone_uidx
    ON auth.users (phone) WHERE phone IS NOT NULL;

-- Email: case-insensitive uniqueness on non-null values
ALTER TABLE auth.users DROP CONSTRAINT IF EXISTS users_email_key;
CREATE UNIQUE INDEX IF NOT EXISTS users_email_ci_uidx
    ON auth.users (LOWER(email)) WHERE email IS NOT NULL;
