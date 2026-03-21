-- Fix: must_haves was TEXT[] but the ListToArrayConverter stores it as a plain
-- TEXT string (e.g. {"wifi","pool"}). Hibernate schema-validation expects VARCHAR/TEXT,
-- not the _text (array) type. Alter to TEXT so the types align.
ALTER TABLE users.taste_profiles
    ALTER COLUMN must_haves TYPE TEXT USING must_haves::TEXT;
