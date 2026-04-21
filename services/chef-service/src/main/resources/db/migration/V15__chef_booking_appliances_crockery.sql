-- Split out of V13 after V13 was already applied in some environments.
-- Editing V13 in place caused a Flyway checksum mismatch, so these two
-- columns live here instead.

ALTER TABLE chefs.chef_bookings
    ADD COLUMN IF NOT EXISTS appliances_json TEXT,
    ADD COLUMN IF NOT EXISTS crockery_json TEXT;
