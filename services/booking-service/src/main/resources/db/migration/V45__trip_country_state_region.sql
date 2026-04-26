-- Multi-country readiness for Universal Trip.
--
-- Adds country / state / region to trip origin and destination so the same
-- engine can be used when Safar expands to UAE / SG / SE Asia / GCC. Strictly
-- additive; existing India-only rows backfilled to 'IN'.
--
-- Country  : ISO-3166-1 alpha-2 (e.g. 'IN', 'AE', 'SG', 'US')
-- State    : ISO-3166-2 subdivision (e.g. 'IN-KA', 'AE-DU')
-- Region   : Safar-defined broader bucket for routing rules
--            (e.g. 'SOUTH_INDIA', 'GULF', 'SE_ASIA') — nullable

ALTER TABLE bookings.trips
    ADD COLUMN IF NOT EXISTS origin_country      VARCHAR(2),
    ADD COLUMN IF NOT EXISTS destination_country VARCHAR(2),
    ADD COLUMN IF NOT EXISTS origin_state        VARCHAR(10),
    ADD COLUMN IF NOT EXISTS destination_state   VARCHAR(10),
    ADD COLUMN IF NOT EXISTS origin_region       VARCHAR(30),
    ADD COLUMN IF NOT EXISTS destination_region  VARCHAR(30);

-- Backfill existing rows to India.
UPDATE bookings.trips
   SET origin_country      = COALESCE(origin_country,      'IN'),
       destination_country = COALESCE(destination_country, 'IN');

-- Make country fields non-null going forward (state + region remain nullable
-- because some bookings may not surface state-level metadata from providers).
ALTER TABLE bookings.trips
    ALTER COLUMN origin_country      SET NOT NULL,
    ALTER COLUMN destination_country SET NOT NULL,
    ALTER COLUMN origin_country      SET DEFAULT 'IN',
    ALTER COLUMN destination_country SET DEFAULT 'IN';

CREATE INDEX IF NOT EXISTS idx_trips_origin_country
    ON bookings.trips (origin_country);
CREATE INDEX IF NOT EXISTS idx_trips_destination_country
    ON bookings.trips (destination_country);
CREATE INDEX IF NOT EXISTS idx_trips_country_pair
    ON bookings.trips (origin_country, destination_country);
