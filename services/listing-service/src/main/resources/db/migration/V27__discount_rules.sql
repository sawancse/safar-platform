-- V27: Weekly and monthly discount rules for listings
ALTER TABLE listings.listings ADD COLUMN IF NOT EXISTS weekly_discount_percent INTEGER;
ALTER TABLE listings.listings ADD COLUMN IF NOT EXISTS monthly_discount_percent INTEGER;
