-- Enforce unique phone across chef profiles.
-- Dedupe existing rows: keep the oldest per phone, null out phone on newer duplicates.
UPDATE chefs.chef_profiles cp
SET phone = NULL
WHERE phone IS NOT NULL
  AND EXISTS (
    SELECT 1 FROM chefs.chef_profiles older
    WHERE older.phone = cp.phone
      AND older.created_at < cp.created_at
  );

-- Partial unique index — NULL phones are ignored, so unregistered chefs aren't blocked.
CREATE UNIQUE INDEX IF NOT EXISTS ux_chef_profiles_phone
  ON chefs.chef_profiles (phone)
  WHERE phone IS NOT NULL;
