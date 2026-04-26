-- V19: Phase B — Platform-managed staff pool.
-- Makes staff_members.chef_id nullable so admins can add staff that
-- aren't tied to a specific chef (chef_id IS NULL = pool member).
-- Chefs can pick from the pool when assigning staff to event bookings.

ALTER TABLE chefs.staff_members
  ALTER COLUMN chef_id DROP NOT NULL;

-- Index tailored to the pool-lookup query (role + active, chef_id null).
CREATE INDEX IF NOT EXISTS idx_staff_members_pool
  ON chefs.staff_members (role, active)
  WHERE chef_id IS NULL;

-- Event-day metadata on assignments.
-- rating_comment lets customers leave a short note when they rate.
ALTER TABLE chefs.event_booking_staff
  ADD COLUMN IF NOT EXISTS rated_at       TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS rating_comment TEXT;
