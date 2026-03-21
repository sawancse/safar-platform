-- Add max_stay_nights to availability table
ALTER TABLE listings.availability ADD COLUMN IF NOT EXISTS max_stay_nights INTEGER;
