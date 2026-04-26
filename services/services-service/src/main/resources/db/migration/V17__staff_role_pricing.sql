-- V17: Per-role staff pricing (Waiter / Cleaner / Bartender)
-- Replaces the single flat ₹1,500/staff rate with role-specific rates so
-- customers can pick how many of each role they need.

-- 1) Seed role pricing rows into event_pricing_defaults.
--    Using WHERE NOT EXISTS so the migration is re-runnable if someone
--    backs it out and replays.
INSERT INTO chefs.event_pricing_defaults
  (category, item_key, label, description, icon,
   default_price_paise, price_type, min_price_paise, max_price_paise, sort_order)
SELECT * FROM (VALUES
  ('STAFF_ROLE', 'waiter',    'Waiter',    'Serves food & drinks, clears plates',        '🧑‍🍳',  99900, 'PER_PERSON',  60000, 200000, 1),
  ('STAFF_ROLE', 'cleaner',   'Cleaner',   'Setup & cleanup before, during and after',   '🧹',   99900, 'PER_PERSON',  60000, 150000, 2),
  ('STAFF_ROLE', 'bartender', 'Bartender', 'Makes cocktails, mocktails, serves drinks',  '🍸',  256900, 'PER_PERSON', 150000, 400000, 3)
) AS v(category, item_key, label, description, icon,
       default_price_paise, price_type, min_price_paise, max_price_paise, sort_order)
WHERE NOT EXISTS (
  SELECT 1 FROM chefs.event_pricing_defaults e
  WHERE e.category = v.category AND e.item_key = v.item_key
);

-- 2) Add a column on event_bookings to store the per-role count map,
--    e.g. {"waiter": 2, "cleaner": 1, "bartender": 1}.
--    The legacy staff_count / staff_paise columns stay for backward
--    compatibility with bookings created before this migration.
ALTER TABLE chefs.event_bookings
  ADD COLUMN IF NOT EXISTS staff_roles_json TEXT;
