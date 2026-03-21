-- Fix avg_rating column type: NUMERIC(3,2) → DOUBLE PRECISION to match entity mapping
ALTER TABLE listings.listings
    ALTER COLUMN avg_rating TYPE DOUBLE PRECISION USING avg_rating::DOUBLE PRECISION;
