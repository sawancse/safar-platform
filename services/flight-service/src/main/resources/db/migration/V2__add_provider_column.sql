-- Persist which provider issued each booking so cancel() routes to the right adapter
ALTER TABLE flights.flight_bookings
    ADD COLUMN IF NOT EXISTS provider VARCHAR(20);

-- Backfill existing rows. Old rows pre-Duffel were Amadeus.
UPDATE flights.flight_bookings
   SET provider = 'AMADEUS'
 WHERE provider IS NULL;

-- Make non-null going forward.
ALTER TABLE flights.flight_bookings
    ALTER COLUMN provider SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_flight_bookings_provider
    ON flights.flight_bookings (provider);
