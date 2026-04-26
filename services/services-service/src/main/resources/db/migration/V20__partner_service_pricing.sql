-- V20: Move partner services (photographer, DJ, pandit, etc.) out of
-- hardcoded frontend arrays into event_pricing_defaults so admins can
-- tune rates without a code push. Each row stores a midpoint price
-- (default_price_paise) and a low/high band for the range string the
-- customer sees. Frontend will render whichever rows come back from
-- /api/v1/chef-events/pricing and fall back to its legacy array only
-- if the API is unreachable.

INSERT INTO chefs.event_pricing_defaults
  (category, item_key, label, description, icon,
   default_price_paise, price_type, min_price_paise, max_price_paise, sort_order)
SELECT * FROM (VALUES
  ('PARTNER_SERVICE', 'photography',    'Photographer',         'Candid + posed photos for the event',           '📷', 1500000, 'PER_EVENT',   500000,  2500000, 1),
  ('PARTNER_SERVICE', 'videography',    'Videographer',         'Highlight reel / full event video',             '🎥', 2400000, 'PER_EVENT',   800000,  4000000, 2),
  ('PARTNER_SERVICE', 'decoration_pro', 'Premium Decor',        'Flowers, stage, backdrop, lighting',            '🌸', 3500000, 'PER_EVENT',  1000000,  6000000, 3),
  ('PARTNER_SERVICE', 'dj',             'DJ & Sound',           'Music + sound system + lights',                 '🎧', 1900000, 'PER_EVENT',   800000,  3000000, 4),
  ('PARTNER_SERVICE', 'live_music',     'Live music / band',    'Sitar, flute, ghazal or small band',            '🎺', 3500000, 'PER_EVENT',  1000000,  6000000, 5),
  ('PARTNER_SERVICE', 'mc',             'MC / Host',            'Anchor to run the evening',                     '🎤', 1250000, 'PER_EVENT',   500000,  2000000, 6),
  ('PARTNER_SERVICE', 'makeup',         'Makeup artist',        'For the couple / guest of honour',              '💄',  900000, 'PER_EVENT',   300000,  1500000, 7),
  ('PARTNER_SERVICE', 'mehndi',         'Mehndi artist',        'Birthdays, anniversaries, weddings',            '🎨',  600000, 'PER_EVENT',   200000,  1000000, 8),
  ('PARTNER_SERVICE', 'pandit',         'Pandit / Puja',        'Silver/gold/diamond jubilee puja',              '🪔',  750000, 'PER_EVENT',   300000,  1200000, 9),
  ('PARTNER_SERVICE', 'bouquet',        'Bouquet / gifts',      'Anniversary flowers, curated hampers',          '💐',  300000, 'PER_EVENT',   100000,   500000, 10),
  ('PARTNER_SERVICE', 'cake_designer',  'Designer cake',        'Tiered / photo-print / sugar-free',             '🎂',  600000, 'PER_EVENT',   200000,  1000000, 11),
  ('PARTNER_SERVICE', 'entertainer',    'Magician / Games',     'For kids-friendly anniversaries',               '🎩', 1250000, 'PER_EVENT',   500000,  2000000, 12),
  ('PARTNER_SERVICE', 'valet',          'Valet / Parking',      'Valet + marshals for guest cars',               '🚗',  650000, 'PER_EVENT',   300000,  1000000, 13)
) AS v(category, item_key, label, description, icon,
       default_price_paise, price_type, min_price_paise, max_price_paise, sort_order)
WHERE NOT EXISTS (
  SELECT 1 FROM chefs.event_pricing_defaults e
  WHERE e.category = v.category AND e.item_key = v.item_key
);
