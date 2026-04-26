-- Trip needs IATA codes (e.g. 'BLR', 'BOM') in addition to city names
-- ('Bangalore', 'Mumbai') so the TripIntentEvaluator can match
-- DESTINATION and ROUTE rules — those rules use IATA codes per Tree-5
-- of the TBO design.
--
-- Without this, evaluateForTrip() falls into the FALLBACK rule and
-- the cross-vertical engine returns STAY-only suggestions for every trip.

ALTER TABLE bookings.trips
    ADD COLUMN IF NOT EXISTS origin_code      VARCHAR(5),
    ADD COLUMN IF NOT EXISTS destination_code VARCHAR(5);

CREATE INDEX IF NOT EXISTS idx_trips_origin_code
    ON bookings.trips (origin_code);
CREATE INDEX IF NOT EXISTS idx_trips_destination_code
    ON bookings.trips (destination_code);
