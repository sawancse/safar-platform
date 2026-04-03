-- V27: Backfill created_at for profiles where it is null
UPDATE users.user_profiles
SET created_at = COALESCE(updated_at, NOW())
WHERE created_at IS NULL;

-- Set default for future inserts
ALTER TABLE users.user_profiles ALTER COLUMN created_at SET DEFAULT NOW();
