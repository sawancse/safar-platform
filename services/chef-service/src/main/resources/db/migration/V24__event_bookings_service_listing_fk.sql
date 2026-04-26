-- V24: Goal B — booking row points to the exact service_item booked, so the
-- customer dashboard can render "the cake/puja/singer you booked" with a deep
-- link back to the storefront.
--
-- The pre-existing menu_description JSON snapshot stays as immutable booking-
-- time data; service_listing_id + service_item_id are the canonical FK pair
-- that drives the deep-link UI.
--
-- service_item_id is NULLABLE because catalog-driven types (cake/decor/pandit/
-- appliance) populate it, while singer/staff/cook bookings have no item — the
-- vendor IS the item. Booking detail UI handles both shapes (Etsy-style shop
-- vs. WedMeGood-style profile-as-listing).

ALTER TABLE chefs.event_bookings
    ADD COLUMN IF NOT EXISTS service_listing_id UUID,
    ADD COLUMN IF NOT EXISTS service_item_id    UUID;

CREATE INDEX IF NOT EXISTS idx_event_bookings_service_listing
    ON chefs.event_bookings(service_listing_id);

CREATE INDEX IF NOT EXISTS idx_event_bookings_service_item
    ON chefs.event_bookings(service_item_id) WHERE service_item_id IS NOT NULL;

-- chef_bookings (single cook visit) doesn't need service_item_id because cook
-- bookings target the chef profile directly, not a catalog item. service_listing_id
-- gets populated on chef_bookings only after V25 backfill (Sprint 4) when chef
-- profiles migrate to first-class service_listings rows.

ALTER TABLE chefs.chef_bookings
    ADD COLUMN IF NOT EXISTS service_listing_id UUID;

CREATE INDEX IF NOT EXISTS idx_chef_bookings_service_listing
    ON chefs.chef_bookings(service_listing_id);
