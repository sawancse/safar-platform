-- New media types and listing special media fields
ALTER TABLE listings.listings ADD COLUMN IF NOT EXISTS floor_plan_url VARCHAR(500);
ALTER TABLE listings.listings ADD COLUMN IF NOT EXISTS panorama_url VARCHAR(500);
ALTER TABLE listings.listings ADD COLUMN IF NOT EXISTS video_tour_url VARCHAR(500);
ALTER TABLE listings.listings ADD COLUMN IF NOT EXISTS neighborhood_photo_urls TEXT;
