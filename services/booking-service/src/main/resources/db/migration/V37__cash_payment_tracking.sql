-- Cash payment tracking for PAY_AT_PROPERTY bookings
ALTER TABLE bookings.bookings
    ADD COLUMN IF NOT EXISTS cash_collected_paise BIGINT,
    ADD COLUMN IF NOT EXISTS cash_collection_note VARCHAR(500);
