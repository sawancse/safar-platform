-- V36: PG/Co-living and Hotel specific fields

-- PG/Co-living fields
ALTER TABLE listings.listings ADD COLUMN occupancy_type VARCHAR(10);
ALTER TABLE listings.listings ADD COLUMN food_type VARCHAR(10);
ALTER TABLE listings.listings ADD COLUMN gate_closing_time TIME;
ALTER TABLE listings.listings ADD COLUMN notice_period_days INTEGER;
ALTER TABLE listings.listings ADD COLUMN security_deposit_paise BIGINT;

-- Hotel fields
ALTER TABLE listings.listings ADD COLUMN hotel_chain VARCHAR(100);
ALTER TABLE listings.listings ADD COLUMN front_desk_24h BOOLEAN DEFAULT FALSE;
ALTER TABLE listings.listings ADD COLUMN checkout_time TIME;
ALTER TABLE listings.listings ADD COLUMN checkin_time TIME;
