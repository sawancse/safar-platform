ALTER TABLE bookings.bookings ADD COLUMN host_earnings_paise BIGINT;
ALTER TABLE bookings.bookings ADD COLUMN platform_fee_paise BIGINT;
ALTER TABLE bookings.bookings ADD COLUMN cleaning_fee_paise BIGINT DEFAULT 0;
ALTER TABLE bookings.bookings ADD COLUMN commission_rate DECIMAL(5,4);
