-- V14: expand dish_catalog with South Indian, Indo-Chinese, regional mains,
-- street snacks, more drinks and desserts. Uses ON CONFLICT DO NOTHING via
-- pre-insert guards so re-running (or running in a partially seeded env) is safe.

-- ── SOUPS_BEVERAGES ───────────────────────────────────────────────────
INSERT INTO chefs.dish_catalog (name, category, price_paise, is_veg, is_recommended, no_onion_garlic, is_fried, sort_order)
SELECT * FROM (VALUES
  ('Rasam',                 'SOUPS_BEVERAGES', 5000, TRUE, TRUE,  TRUE, FALSE, 11),
  ('Mulligatawny Soup',     'SOUPS_BEVERAGES', 7000, TRUE, FALSE, TRUE, FALSE, 12),
  ('Tom Yum Veg Soup',      'SOUPS_BEVERAGES', 8000, TRUE, FALSE, FALSE,FALSE, 13),
  ('Mint Mojito',           'SOUPS_BEVERAGES', 9000, TRUE, TRUE,  TRUE, FALSE, 14),
  ('Fresh Lime Soda',       'SOUPS_BEVERAGES', 5000, TRUE, FALSE, TRUE, FALSE, 15),
  ('Watermelon Juice',      'SOUPS_BEVERAGES', 6000, TRUE, FALSE, TRUE, FALSE, 16),
  ('Badam Milk',            'SOUPS_BEVERAGES', 7000, TRUE, FALSE, TRUE, FALSE, 17),
  ('Sweet Lassi',           'SOUPS_BEVERAGES', 6000, TRUE, TRUE,  TRUE, FALSE, 18),
  ('Salted Buttermilk',     'SOUPS_BEVERAGES', 4000, TRUE, FALSE, TRUE, FALSE, 19),
  ('Masala Chai',           'SOUPS_BEVERAGES', 3000, TRUE, TRUE,  TRUE, FALSE, 20),
  ('Filter Coffee',         'SOUPS_BEVERAGES', 4000, TRUE, TRUE,  TRUE, FALSE, 21),
  ('Aam Panna',             'SOUPS_BEVERAGES', 5000, TRUE, FALSE, TRUE, FALSE, 22)
) AS v(name, category, price_paise, is_veg, is_recommended, no_onion_garlic, is_fried, sort_order)
WHERE NOT EXISTS (SELECT 1 FROM chefs.dish_catalog d WHERE d.name = v.name AND d.category = v.category);

-- ── APPETIZERS ────────────────────────────────────────────────────────
INSERT INTO chefs.dish_catalog (name, category, price_paise, is_veg, is_recommended, no_onion_garlic, is_fried, sort_order)
SELECT * FROM (VALUES
  ('Gobi Manchurian',       'APPETIZERS', 14000, TRUE,  TRUE,  FALSE, TRUE,  13),
  ('Paneer 65',             'APPETIZERS', 16000, TRUE,  TRUE,  FALSE, TRUE,  14),
  ('Crispy Corn',           'APPETIZERS', 12000, TRUE,  FALSE, FALSE, TRUE,  15),
  ('Mushroom Tikka',        'APPETIZERS', 14000, TRUE,  FALSE, FALSE, FALSE, 16),
  ('Soya Chaap Tikka',      'APPETIZERS', 15000, TRUE,  FALSE, FALSE, FALSE, 17),
  ('Veg Manchurian',        'APPETIZERS', 12000, TRUE,  FALSE, FALSE, TRUE,  18),
  ('Pav Bhaji',             'APPETIZERS', 12000, TRUE,  TRUE,  FALSE, FALSE, 19),
  ('Bhel Puri',             'APPETIZERS',  8000, TRUE,  TRUE,  FALSE, FALSE, 20),
  ('Dahi Puri',             'APPETIZERS',  8000, TRUE,  TRUE,  FALSE, FALSE, 21),
  ('Pani Puri',             'APPETIZERS',  8000, TRUE,  TRUE,  FALSE, FALSE, 22),
  ('Vada Pav',              'APPETIZERS',  6000, TRUE,  FALSE, FALSE, TRUE,  23),
  ('Chicken 65',            'APPETIZERS', 18000, FALSE, TRUE,  FALSE, TRUE,  24),
  ('Honey Chilli Chicken',  'APPETIZERS', 20000, FALSE, TRUE,  FALSE, TRUE,  25),
  ('Chicken Lollipop',      'APPETIZERS', 20000, FALSE, FALSE, FALSE, TRUE,  26),
  ('Fish Tikka',            'APPETIZERS', 20000, FALSE, FALSE, FALSE, FALSE, 27),
  ('Kathi Roll (Chicken)',  'APPETIZERS', 15000, FALSE, FALSE, FALSE, FALSE, 28)
) AS v(name, category, price_paise, is_veg, is_recommended, no_onion_garlic, is_fried, sort_order)
WHERE NOT EXISTS (SELECT 1 FROM chefs.dish_catalog d WHERE d.name = v.name AND d.category = v.category);

-- ── MAIN_COURSE ───────────────────────────────────────────────────────
INSERT INTO chefs.dish_catalog (name, category, price_paise, is_veg, is_recommended, no_onion_garlic, is_fried, sort_order)
SELECT * FROM (VALUES
  ('Sambhar',                  'MAIN_COURSE', 10000, TRUE,  TRUE,  TRUE,  FALSE, 19),
  ('Dal Tadka',                'MAIN_COURSE', 12000, TRUE,  TRUE,  TRUE,  FALSE, 20),
  ('Dal Fry',                  'MAIN_COURSE', 12000, TRUE,  FALSE, TRUE,  FALSE, 21),
  ('Chana Masala',             'MAIN_COURSE', 14000, TRUE,  TRUE,  FALSE, FALSE, 22),
  ('Shahi Paneer',             'MAIN_COURSE', 18000, TRUE,  TRUE,  FALSE, FALSE, 23),
  ('Matar Paneer',             'MAIN_COURSE', 16000, TRUE,  FALSE, FALSE, FALSE, 24),
  ('Paneer Lababdar',          'MAIN_COURSE', 18000, TRUE,  FALSE, FALSE, FALSE, 25),
  ('Paneer Bhurji',            'MAIN_COURSE', 15000, TRUE,  FALSE, FALSE, FALSE, 26),
  ('Bhindi Masala',            'MAIN_COURSE', 14000, TRUE,  FALSE, TRUE,  FALSE, 27),
  ('Methi Malai Matar',        'MAIN_COURSE', 16000, TRUE,  FALSE, FALSE, FALSE, 28),
  ('Lauki Kofta',              'MAIN_COURSE', 15000, TRUE,  FALSE, TRUE,  TRUE,  29),
  ('Avial',                    'MAIN_COURSE', 14000, TRUE,  TRUE,  TRUE,  FALSE, 30),
  ('Kerala Veg Stew',          'MAIN_COURSE', 14000, TRUE,  FALSE, TRUE,  FALSE, 31),
  ('Kerala Fish Curry',        'MAIN_COURSE', 22000, FALSE, TRUE,  FALSE, FALSE, 32),
  ('Chicken Chettinad',        'MAIN_COURSE', 22000, FALSE, TRUE,  FALSE, FALSE, 33),
  ('Chicken Korma',            'MAIN_COURSE', 22000, FALSE, FALSE, FALSE, FALSE, 34),
  ('Goan Fish Curry',          'MAIN_COURSE', 22000, FALSE, FALSE, FALSE, FALSE, 35),
  ('Goan Prawn Curry',         'MAIN_COURSE', 26000, FALSE, FALSE, FALSE, FALSE, 36),
  ('Hyderabadi Chicken Biryani','MAIN_COURSE', 24000, FALSE, TRUE,  FALSE, FALSE, 37),
  ('Andhra Chicken Curry',     'MAIN_COURSE', 22000, FALSE, FALSE, FALSE, FALSE, 38)
) AS v(name, category, price_paise, is_veg, is_recommended, no_onion_garlic, is_fried, sort_order)
WHERE NOT EXISTS (SELECT 1 FROM chefs.dish_catalog d WHERE d.name = v.name AND d.category = v.category);

-- ── BREADS ────────────────────────────────────────────────────────────
INSERT INTO chefs.dish_catalog (name, category, price_paise, is_veg, is_recommended, no_onion_garlic, is_fried, sort_order)
SELECT * FROM (VALUES
  ('Chapati',          'BREADS', 3000, TRUE, FALSE, TRUE,  FALSE, 9),
  ('Phulka',           'BREADS', 3000, TRUE, FALSE, TRUE,  FALSE, 10),
  ('Poori',            'BREADS', 4000, TRUE, TRUE,  TRUE,  TRUE,  11),
  ('Bhatura',          'BREADS', 6000, TRUE, TRUE,  TRUE,  TRUE,  12),
  ('Methi Paratha',    'BREADS', 6000, TRUE, FALSE, TRUE,  FALSE, 13),
  ('Pyaaz Paratha',    'BREADS', 6000, TRUE, FALSE, FALSE, FALSE, 14),
  ('Cheese Naan',      'BREADS', 8000, TRUE, TRUE,  TRUE,  FALSE, 15),
  ('Rumali Roti',      'BREADS', 5000, TRUE, FALSE, TRUE,  FALSE, 16),
  ('Parotta (Malabar)','BREADS', 5000, TRUE, FALSE, TRUE,  FALSE, 17)
) AS v(name, category, price_paise, is_veg, is_recommended, no_onion_garlic, is_fried, sort_order)
WHERE NOT EXISTS (SELECT 1 FROM chefs.dish_catalog d WHERE d.name = v.name AND d.category = v.category);

-- ── RICE ──────────────────────────────────────────────────────────────
INSERT INTO chefs.dish_catalog (name, category, price_paise, is_veg, is_recommended, no_onion_garlic, is_fried, sort_order)
SELECT * FROM (VALUES
  ('Ghee Rice',            'RICE', 7000,  TRUE,  TRUE,  TRUE,  FALSE, 6),
  ('Bisi Bele Bath',       'RICE', 9000,  TRUE,  TRUE,  TRUE,  FALSE, 7),
  ('Coconut Rice',         'RICE', 7000,  TRUE,  FALSE, TRUE,  FALSE, 8),
  ('Tamarind Rice',        'RICE', 7000,  TRUE,  FALSE, TRUE,  FALSE, 9),
  ('Tomato Rice',          'RICE', 7000,  TRUE,  FALSE, TRUE,  FALSE, 10),
  ('Veg Fried Rice',       'RICE', 9000,  TRUE,  TRUE,  FALSE, TRUE,  11),
  ('Schezwan Fried Rice',  'RICE', 10000, TRUE,  FALSE, FALSE, TRUE,  12),
  ('Egg Fried Rice',       'RICE', 11000, FALSE, FALSE, FALSE, TRUE,  13),
  ('Chicken Fried Rice',   'RICE', 13000, FALSE, TRUE,  FALSE, TRUE,  14),
  ('Hyderabadi Veg Biryani','RICE',12000, TRUE,  TRUE,  FALSE, FALSE, 15)
) AS v(name, category, price_paise, is_veg, is_recommended, no_onion_garlic, is_fried, sort_order)
WHERE NOT EXISTS (SELECT 1 FROM chefs.dish_catalog d WHERE d.name = v.name AND d.category = v.category);

-- ── RAITA ─────────────────────────────────────────────────────────────
INSERT INTO chefs.dish_catalog (name, category, price_paise, is_veg, is_recommended, no_onion_garlic, is_fried, sort_order)
SELECT * FROM (VALUES
  ('Cucumber Raita',   'RAITA', 4000, TRUE, TRUE,  TRUE,  FALSE, 5),
  ('Palak Raita',      'RAITA', 4000, TRUE, FALSE, TRUE,  FALSE, 6),
  ('Beetroot Raita',   'RAITA', 4000, TRUE, FALSE, TRUE,  FALSE, 7),
  ('Mint Raita',       'RAITA', 4000, TRUE, FALSE, TRUE,  FALSE, 8)
) AS v(name, category, price_paise, is_veg, is_recommended, no_onion_garlic, is_fried, sort_order)
WHERE NOT EXISTS (SELECT 1 FROM chefs.dish_catalog d WHERE d.name = v.name AND d.category = v.category);

-- ── DESSERTS ──────────────────────────────────────────────────────────
INSERT INTO chefs.dish_catalog (name, category, price_paise, is_veg, is_recommended, no_onion_garlic, is_fried, sort_order)
SELECT * FROM (VALUES
  ('Rabri',             'DESSERTS',  9000, TRUE, FALSE, TRUE, FALSE, 9),
  ('Kulfi',             'DESSERTS',  8000, TRUE, TRUE,  TRUE, FALSE, 10),
  ('Shahi Tukda',       'DESSERTS', 12000, TRUE, TRUE,  TRUE, TRUE,  11),
  ('Malpua',            'DESSERTS', 10000, TRUE, FALSE, TRUE, TRUE,  12),
  ('Besan Ladoo',       'DESSERTS',  6000, TRUE, FALSE, TRUE, FALSE, 13),
  ('Motichoor Ladoo',   'DESSERTS',  7000, TRUE, FALSE, TRUE, FALSE, 14),
  ('Mysore Pak',        'DESSERTS',  7000, TRUE, TRUE,  TRUE, FALSE, 15),
  ('Sheera',            'DESSERTS',  6000, TRUE, FALSE, TRUE, FALSE, 16),
  ('Semiya Payasam',    'DESSERTS',  8000, TRUE, TRUE,  TRUE, FALSE, 17),
  ('Kaju Katli',        'DESSERTS', 10000, TRUE, TRUE,  TRUE, FALSE, 18),
  ('Coconut Barfi',     'DESSERTS',  7000, TRUE, FALSE, TRUE, FALSE, 19),
  ('Mango Kulfi',       'DESSERTS',  9000, TRUE, FALSE, TRUE, FALSE, 20)
) AS v(name, category, price_paise, is_veg, is_recommended, no_onion_garlic, is_fried, sort_order)
WHERE NOT EXISTS (SELECT 1 FROM chefs.dish_catalog d WHERE d.name = v.name AND d.category = v.category);
