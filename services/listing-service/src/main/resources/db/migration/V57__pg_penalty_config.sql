-- Configurable grace period and late penalty per PG listing (5-day payment window: 1st-5th)
ALTER TABLE listings.listings ADD COLUMN IF NOT EXISTS grace_period_days INT DEFAULT 5;
ALTER TABLE listings.listings ADD COLUMN IF NOT EXISTS late_penalty_bps INT DEFAULT 200;
