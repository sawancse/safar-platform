-- Add review aggregation columns to listings table
ALTER TABLE listings.listings
    ADD COLUMN IF NOT EXISTS avg_rating    NUMERIC(3,2) NOT NULL DEFAULT 0.0,
    ADD COLUMN IF NOT EXISTS review_count  INTEGER      NOT NULL DEFAULT 0;
