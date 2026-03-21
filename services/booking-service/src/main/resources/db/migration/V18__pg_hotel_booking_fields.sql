-- V18: PG/Hotel booking fields
ALTER TABLE bookings.bookings ADD COLUMN notice_period_days INTEGER;
ALTER TABLE bookings.bookings ADD COLUMN security_deposit_paise BIGINT;
ALTER TABLE bookings.bookings ADD COLUMN security_deposit_status VARCHAR(20);
