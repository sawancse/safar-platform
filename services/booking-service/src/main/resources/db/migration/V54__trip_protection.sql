-- V54: embedded trip-protection premium folded into the booking total.
-- Premium is fetched server-side from insurance-service (client can't set the amount).
ALTER TABLE bookings.bookings
    ADD COLUMN IF NOT EXISTS trip_protection_paise    BIGINT,
    ADD COLUMN IF NOT EXISTS trip_protection_quote_id VARCHAR(80);
