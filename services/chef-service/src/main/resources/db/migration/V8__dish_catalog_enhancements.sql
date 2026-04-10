-- V8: Dish catalog enhancements — public dish catalog + cook matching

-- ── Enhance menu_items with pricing and dietary flags ─────────────────
ALTER TABLE chefs.menu_items ADD COLUMN IF NOT EXISTS price_paise BIGINT;
ALTER TABLE chefs.menu_items ADD COLUMN IF NOT EXISTS photo_url VARCHAR(500);
ALTER TABLE chefs.menu_items ADD COLUMN IF NOT EXISTS is_recommended BOOLEAN DEFAULT FALSE;
ALTER TABLE chefs.menu_items ADD COLUMN IF NOT EXISTS no_onion_garlic BOOLEAN DEFAULT FALSE;
ALTER TABLE chefs.menu_items ADD COLUMN IF NOT EXISTS is_fried BOOLEAN DEFAULT FALSE;

-- ── Add gas_burners to chef_profiles ──────────────────────────────────
ALTER TABLE chefs.chef_profiles ADD COLUMN IF NOT EXISTS gas_burners INT DEFAULT 2;

-- ── Global public dish catalog (NOT tied to a specific menu) ──────────
CREATE TABLE IF NOT EXISTS chefs.dish_catalog (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(200) NOT NULL,
    description TEXT,
    category    VARCHAR(50)  NOT NULL,
    price_paise BIGINT       NOT NULL DEFAULT 0,
    photo_url   VARCHAR(500),
    is_veg      BOOLEAN DEFAULT TRUE,
    is_recommended BOOLEAN DEFAULT FALSE,
    no_onion_garlic BOOLEAN DEFAULT FALSE,
    is_fried    BOOLEAN DEFAULT FALSE,
    sort_order  INT DEFAULT 0,
    active      BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_dish_catalog_category_active
    ON chefs.dish_catalog (category, active);

-- ── Chef ↔ Dish join table (which chef can cook which catalog dish) ───
CREATE TABLE IF NOT EXISTS chefs.chef_dish_offerings (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chef_id           UUID NOT NULL REFERENCES chefs.chef_profiles(id),
    dish_id           UUID NOT NULL REFERENCES chefs.dish_catalog(id),
    custom_price_paise BIGINT,
    created_at        TIMESTAMPTZ DEFAULT now(),
    UNIQUE (chef_id, dish_id)
);

CREATE INDEX IF NOT EXISTS idx_chef_dish_offerings_chef_id
    ON chefs.chef_dish_offerings (chef_id);

-- ══════════════════════════════════════════════════════════════════════
-- Seed dish_catalog with real Indian dishes
-- ══════════════════════════════════════════════════════════════════════

-- ── SOUPS_BEVERAGES ───────────────────────────────────────────────────
INSERT INTO chefs.dish_catalog (name, category, price_paise, is_veg, is_recommended, no_onion_garlic, is_fried, sort_order) VALUES
('Tomato Soup',           'SOUPS_BEVERAGES', 5000, TRUE,  FALSE, TRUE,  FALSE, 1),
('Sweet Corn Soup',       'SOUPS_BEVERAGES', 6000, TRUE,  FALSE, TRUE,  FALSE, 2),
('Hot & Sour Soup',       'SOUPS_BEVERAGES', 6000, TRUE,  FALSE, FALSE, FALSE, 3),
('Manchow Soup',          'SOUPS_BEVERAGES', 7000, TRUE,  FALSE, FALSE, FALSE, 4),
('Cream of Mushroom',     'SOUPS_BEVERAGES', 7000, TRUE,  FALSE, TRUE,  FALSE, 5),
('Lemon Coriander Soup',  'SOUPS_BEVERAGES', 5000, TRUE,  FALSE, TRUE,  FALSE, 6),
('Masala Chaas',           'SOUPS_BEVERAGES', 5000, TRUE,  FALSE, FALSE, FALSE, 7),
('Jaljeera',               'SOUPS_BEVERAGES', 5000, TRUE,  TRUE,  TRUE,  FALSE, 8),
('Mango Lassi',            'SOUPS_BEVERAGES', 6000, TRUE,  TRUE,  TRUE,  FALSE, 9),
('Cold Coffee',            'SOUPS_BEVERAGES', 8000, TRUE,  FALSE, TRUE,  FALSE, 10);

-- ── APPETIZERS ────────────────────────────────────────────────────────
INSERT INTO chefs.dish_catalog (name, category, price_paise, is_veg, is_recommended, no_onion_garlic, is_fried, sort_order) VALUES
('Paneer Tikka',       'APPETIZERS', 15000, TRUE,  TRUE,  FALSE, FALSE, 1),
('Hara Bhara Kebab',   'APPETIZERS', 12000, TRUE,  FALSE, FALSE, FALSE, 2),
('Dahi Ke Sholay',     'APPETIZERS', 12000, TRUE,  FALSE, FALSE, TRUE,  3),
('Veg Spring Rolls',   'APPETIZERS', 10000, TRUE,  FALSE, FALSE, TRUE,  4),
('Aloo Tikki',         'APPETIZERS', 10000, TRUE,  FALSE, FALSE, TRUE,  5),
('Samosa',             'APPETIZERS', 8000,  TRUE,  TRUE,  FALSE, TRUE,  6),
('Onion Bhajiya',      'APPETIZERS', 8000,  TRUE,  FALSE, FALSE, TRUE,  7),
('Chicken Tikka',      'APPETIZERS', 18000, FALSE, TRUE,  FALSE, FALSE, 8),
('Tandoori Chicken',   'APPETIZERS', 20000, FALSE, TRUE,  FALSE, FALSE, 9),
('Fish Amritsari',     'APPETIZERS', 18000, FALSE, FALSE, FALSE, TRUE,  10),
('Mutton Seekh Kebab', 'APPETIZERS', 20000, FALSE, FALSE, FALSE, FALSE, 11),
('Prawn Koliwada',     'APPETIZERS', 20000, FALSE, FALSE, FALSE, TRUE,  12);

-- ── MAIN_COURSE ───────────────────────────────────────────────────────
INSERT INTO chefs.dish_catalog (name, category, price_paise, is_veg, is_recommended, no_onion_garlic, is_fried, sort_order) VALUES
('Dal Makhani',           'MAIN_COURSE', 15000, TRUE,  TRUE,  TRUE,  FALSE, 1),
('Paneer Butter Masala',  'MAIN_COURSE', 18000, TRUE,  TRUE,  FALSE, FALSE, 2),
('Palak Paneer',          'MAIN_COURSE', 16000, TRUE,  FALSE, FALSE, FALSE, 3),
('Malai Kofta',           'MAIN_COURSE', 18000, TRUE,  FALSE, FALSE, TRUE,  4),
('Chole',                 'MAIN_COURSE', 15000, TRUE,  FALSE, FALSE, FALSE, 5),
('Rajma',                 'MAIN_COURSE', 15000, TRUE,  FALSE, FALSE, FALSE, 6),
('Kadhai Paneer',         'MAIN_COURSE', 18000, TRUE,  FALSE, FALSE, FALSE, 7),
('Mix Veg',               'MAIN_COURSE', 15000, TRUE,  FALSE, FALSE, FALSE, 8),
('Baingan Bharta',        'MAIN_COURSE', 15000, TRUE,  FALSE, FALSE, FALSE, 9),
('Aloo Gobi',             'MAIN_COURSE', 15000, TRUE,  FALSE, TRUE,  FALSE, 10),
('Butter Chicken',        'MAIN_COURSE', 22000, FALSE, TRUE,  FALSE, FALSE, 11),
('Chicken Curry',         'MAIN_COURSE', 20000, FALSE, FALSE, FALSE, FALSE, 12),
('Mutton Rogan Josh',     'MAIN_COURSE', 25000, FALSE, TRUE,  FALSE, FALSE, 13),
('Fish Curry',            'MAIN_COURSE', 20000, FALSE, FALSE, FALSE, FALSE, 14),
('Egg Curry',             'MAIN_COURSE', 15000, FALSE, FALSE, FALSE, FALSE, 15),
('Chicken Biryani',       'MAIN_COURSE', 22000, FALSE, TRUE,  FALSE, FALSE, 16),
('Mutton Biryani',        'MAIN_COURSE', 25000, FALSE, TRUE,  FALSE, FALSE, 17),
('Veg Biryani',           'MAIN_COURSE', 18000, TRUE,  FALSE, FALSE, FALSE, 18);

-- ── BREADS ────────────────────────────────────────────────────────────
INSERT INTO chefs.dish_catalog (name, category, price_paise, is_veg, is_recommended, no_onion_garlic, is_fried, sort_order) VALUES
('Tawa Roti',        'BREADS', 4000, TRUE, FALSE, TRUE,  FALSE, 1),
('Butter Naan',      'BREADS', 5000, TRUE, TRUE,  TRUE,  FALSE, 2),
('Garlic Naan',      'BREADS', 6000, TRUE, TRUE,  FALSE, FALSE, 3),
('Tandoori Roti',    'BREADS', 4000, TRUE, FALSE, TRUE,  FALSE, 4),
('Missi Roti',       'BREADS', 5000, TRUE, FALSE, TRUE,  FALSE, 5),
('Lachha Paratha',   'BREADS', 6000, TRUE, FALSE, TRUE,  FALSE, 6),
('Aloo Paratha',     'BREADS', 8000, TRUE, TRUE,  FALSE, FALSE, 7),
('Kulcha',           'BREADS', 6000, TRUE, FALSE, FALSE, FALSE, 8);

-- ── RICE ──────────────────────────────────────────────────────────────
INSERT INTO chefs.dish_catalog (name, category, price_paise, is_veg, is_recommended, no_onion_garlic, is_fried, sort_order) VALUES
('Steamed Rice',  'RICE', 5000,  TRUE, FALSE, TRUE,  FALSE, 1),
('Jeera Rice',    'RICE', 6000,  TRUE, TRUE,  TRUE,  FALSE, 2),
('Veg Pulao',     'RICE', 8000,  TRUE, FALSE, FALSE, FALSE, 3),
('Lemon Rice',    'RICE', 6000,  TRUE, FALSE, TRUE,  FALSE, 4),
('Curd Rice',     'RICE', 6000,  TRUE, FALSE, TRUE,  FALSE, 5);

-- ── RAITA ─────────────────────────────────────────────────────────────
INSERT INTO chefs.dish_catalog (name, category, price_paise, is_veg, is_recommended, no_onion_garlic, is_fried, sort_order) VALUES
('Boondi Raita',        'RAITA', 4000, TRUE, TRUE,  TRUE,  FALSE, 1),
('Mix Veg Raita',       'RAITA', 4000, TRUE, FALSE, FALSE, FALSE, 2),
('Pineapple Raita',     'RAITA', 5000, TRUE, FALSE, TRUE,  FALSE, 3),
('Onion Tomato Raita',  'RAITA', 3000, TRUE, FALSE, FALSE, FALSE, 4);

-- ── DESSERTS ──────────────────────────────────────────────────────────
INSERT INTO chefs.dish_catalog (name, category, price_paise, is_veg, is_recommended, no_onion_garlic, is_fried, sort_order) VALUES
('Gulab Jamun',       'DESSERTS', 8000,  TRUE, TRUE,  TRUE,  TRUE,  1),
('Rasmalai',          'DESSERTS', 10000, TRUE, TRUE,  TRUE,  FALSE, 2),
('Kheer',             'DESSERTS', 8000,  TRUE, FALSE, TRUE,  FALSE, 3),
('Gajar Ka Halwa',    'DESSERTS', 10000, TRUE, TRUE,  TRUE,  FALSE, 4),
('Moong Dal Halwa',   'DESSERTS', 12000, TRUE, FALSE, TRUE,  FALSE, 5),
('Phirni',            'DESSERTS', 8000,  TRUE, FALSE, TRUE,  FALSE, 6),
('Jalebi',            'DESSERTS', 8000,  TRUE, TRUE,  TRUE,  TRUE,  7),
('Ras Malai',         'DESSERTS', 10000, TRUE, FALSE, TRUE,  FALSE, 8);
