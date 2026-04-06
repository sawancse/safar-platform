-- Allow seller-initiated visits without a buyer (open house / slot-based)
ALTER TABLE listings.site_visits ALTER COLUMN buyer_id DROP NOT NULL;

-- Add notes field for visit instructions
ALTER TABLE listings.site_visits ADD COLUMN IF NOT EXISTS notes TEXT;
