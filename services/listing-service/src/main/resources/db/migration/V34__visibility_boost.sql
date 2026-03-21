ALTER TABLE listings.listings ADD COLUMN visibility_boost_percent INTEGER DEFAULT 0 CHECK (visibility_boost_percent IN (0, 3, 5));
