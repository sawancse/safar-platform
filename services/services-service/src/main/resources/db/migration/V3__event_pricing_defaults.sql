-- ── Event Pricing Defaults ──────────────────────────────────────────────────
CREATE TABLE chefs.event_pricing_defaults (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category            VARCHAR(20) NOT NULL,
    item_key            VARCHAR(50) NOT NULL UNIQUE,
    label               VARCHAR(100) NOT NULL,
    description         VARCHAR(255),
    icon                VARCHAR(10),
    default_price_paise BIGINT NOT NULL,
    price_type          VARCHAR(20) NOT NULL,
    min_price_paise     BIGINT,
    max_price_paise     BIGINT,
    sort_order          INTEGER DEFAULT 0,
    active              BOOLEAN DEFAULT true,
    created_at          TIMESTAMP DEFAULT now(),
    updated_at          TIMESTAMP DEFAULT now()
);

-- ── Chef Event Pricing Overrides ───────────────────────────────────────────
CREATE TABLE chefs.chef_event_pricing (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chef_id             UUID NOT NULL REFERENCES chefs.chef_profiles(id),
    item_key            VARCHAR(50) NOT NULL,
    custom_price_paise  BIGINT NOT NULL,
    available           BOOLEAN DEFAULT true,
    created_at          TIMESTAMP DEFAULT now(),
    updated_at          TIMESTAMP DEFAULT now(),
    UNIQUE(chef_id, item_key)
);

-- ── Seed: Base Configs ─────────────────────────────────────────────────────
INSERT INTO chefs.event_pricing_defaults (category, item_key, label, description, icon, default_price_paise, price_type, min_price_paise, max_price_paise, sort_order) VALUES
('BASE_CONFIG', 'per_plate',        'Per Plate Food',       'Base food cost per guest',              '🍛', 30000,  'PER_PLATE',   15000,  100000, 1),
('BASE_CONFIG', 'staff',            'Extra Serving Staff',  'Waiters for serving & cleanup',         '🧑‍🍳', 150000, 'PER_PERSON',  100000, 300000, 2),
('BASE_CONFIG', 'platform_fee_pct', 'Platform Fee',         'Service fee percentage (basis points)', '💰', 1000,   'PERCENTAGE',  500,    2000,   3);

-- ── Seed: Live Counters ────────────────────────────────────────────────────
INSERT INTO chefs.event_pricing_defaults (category, item_key, label, description, icon, default_price_paise, price_type, min_price_paise, max_price_paise, sort_order) VALUES
('LIVE_COUNTER', 'dosa',    'Live Dosa Counter',    'South Indian dosa station',  '🥞', 300000, 'FIXED', 150000, 500000, 1),
('LIVE_COUNTER', 'pasta',   'Live Pasta Counter',   'Italian pasta station',      '🍝', 350000, 'FIXED', 200000, 600000, 2),
('LIVE_COUNTER', 'bbq',     'Live BBQ Counter',     'Barbecue grill station',     '🔥', 500000, 'FIXED', 300000, 800000, 3),
('LIVE_COUNTER', 'chaat',   'Live Chaat Counter',   'Street food chaat station',  '🥗', 250000, 'FIXED', 150000, 400000, 4),
('LIVE_COUNTER', 'tandoor', 'Live Tandoor Counter', 'Tandoori grill station',     '🫓', 400000, 'FIXED', 200000, 700000, 5);

-- ── Seed: Add-ons ──────────────────────────────────────────────────────────
INSERT INTO chefs.event_pricing_defaults (category, item_key, label, description, icon, default_price_paise, price_type, min_price_paise, max_price_paise, sort_order) VALUES
('ADDON', 'decoration',  'Event Decoration',      'Balloons, banners, table setting & theme decor', '🎈', 500000, 'FIXED', 200000, 1500000, 1),
('ADDON', 'cake',        'Designer Cake',         'Custom theme cake (1-2 kg)',                     '🎂', 200000, 'FIXED', 100000, 500000,  2),
('ADDON', 'crockery',    'Crockery Rental',       'Plates, glasses, cutlery, serving bowls',        '🍽️', 80000,  'FIXED', 50000,  200000,  3),
('ADDON', 'appliances',  'Appliance Rental',      'Chafing dishes, gas stoves, induction',          '🔌', 50000,  'FIXED', 30000,  150000,  4),
('ADDON', 'table_setup', 'Fine Dine Table Setup', 'Premium tablecloth, candles, flowers',           '🕯️', 80000,  'FIXED', 50000,  200000,  5);
