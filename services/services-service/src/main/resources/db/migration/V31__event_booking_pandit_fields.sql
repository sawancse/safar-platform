-- V31: Pandit-specific structured booking fields.
--
-- The pandit catalog (frontend) already carries pandit_count implicitly in
-- premium/luxury tiers and a samagri quality level, but these were only ever
-- buried inside event_bookings.menu_description JSON. Promote them to real
-- columns so admin assignment, payouts and pandit-matching can query/aggregate
-- without parsing JSON.
--
--   pandit_count   how many pandits the puja needs (1 for standard, 2-3 for
--                  premium/luxury yagnas). Defaults to 1.
--   samagri_tier   BASIC | STANDARD | PREMIUM — quality of the samagri kit,
--                  lets us decompose/charge for richer kits later.

ALTER TABLE chefs.event_bookings
    ADD COLUMN IF NOT EXISTS pandit_count INT          DEFAULT 1,
    ADD COLUMN IF NOT EXISTS samagri_tier VARCHAR(20);
