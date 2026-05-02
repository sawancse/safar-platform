-- ============================================================================
-- V29: Live tracking columns on event_bookings (mirror chef_bookings V5)
-- ============================================================================
-- Cook bookings (chef_bookings) have had chef_lat/chef_lng/eta_minutes/
-- location_updated_at since V5 — that's what powers the buyer's live tracking
-- panel on /cooks/my-bookings/{id}.
--
-- Event bookings (pandit, decor, cake, singer, staff, appliance) need the same
-- columns so self-service vendors can share their live location too. The
-- buyer-side UI already polls for these fields; without them the vendor has
-- no way to push location and the panel sits "Waiting for the pandit to
-- share location" forever.
-- ============================================================================

ALTER TABLE chefs.event_bookings
    ADD COLUMN IF NOT EXISTS chef_lat              DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS chef_lng              DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS eta_minutes           INTEGER,
    ADD COLUMN IF NOT EXISTS location_updated_at   TIMESTAMPTZ;
