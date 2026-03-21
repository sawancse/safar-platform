-- V50: Hotel booking enhancements
-- New fields for couple-friendly, property highlights, early bird deals, zero-payment booking

ALTER TABLE listings.listings ADD COLUMN IF NOT EXISTS couple_friendly BOOLEAN DEFAULT FALSE;
ALTER TABLE listings.listings ADD COLUMN IF NOT EXISTS property_highlights TEXT; -- comma-separated highlights
ALTER TABLE listings.listings ADD COLUMN IF NOT EXISTS early_bird_discount_percent INTEGER DEFAULT 0;
ALTER TABLE listings.listings ADD COLUMN IF NOT EXISTS early_bird_days_before INTEGER DEFAULT 30; -- book X days early to get discount
ALTER TABLE listings.listings ADD COLUMN IF NOT EXISTS zero_payment_booking BOOLEAN DEFAULT FALSE; -- book with ₹0 upfront
ALTER TABLE listings.listings ADD COLUMN IF NOT EXISTS location_highlight VARCHAR(200); -- e.g. "Excellent location — next to Rajiv Chowk Metro"
