-- Flight search-event capture for abandoned-search recovery campaigns.
--
-- Every search hit on /api/v1/flights/search is recorded here so the
-- AbandonedSearchDetector job (runs every 30 min) can identify users
-- who searched but didn't book, and trigger reminder notifications via
-- the notification-service (Push → WhatsApp → Email cascade).

CREATE TABLE IF NOT EXISTS flights.flight_search_events (
    id                       UUID         PRIMARY KEY,
    user_id                  UUID,                          -- nullable: anonymous searches captured by device_id
    device_id                VARCHAR(80),                   -- cookie/localStorage UUID for anonymous identity
    origin                   VARCHAR(5)   NOT NULL,
    destination              VARCHAR(5)   NOT NULL,
    departure_date           DATE         NOT NULL,
    return_date              DATE,
    pax_count                INT          NOT NULL DEFAULT 1,
    cabin_class              VARCHAR(20),
    -- Origin/destination country pair — multi-country ready (matches Trip schema design)
    origin_country           VARCHAR(2)   NOT NULL DEFAULT 'IN',
    destination_country      VARCHAR(2)   NOT NULL DEFAULT 'IN',
    -- Cheapest fare seen at search time; used by detector to compute fare-trend signal
    cheapest_fare_paise      BIGINT,
    currency                 VARCHAR(3)   NOT NULL DEFAULT 'INR',
    -- Reminder tracking: cap at 3 pulses (1h, 6h, 24h)
    reminders_sent           INT          NOT NULL DEFAULT 0,
    last_reminder_at         TIMESTAMPTZ,
    -- Suppression flags — short-circuit the detector
    suppressed               BOOLEAN      NOT NULL DEFAULT FALSE,
    suppression_reason       VARCHAR(40),                   -- BOOKED / DATE_PASSED / OPT_OUT / MAX_REMINDERS / EXPIRED
    contact_email            VARCHAR(200),                  -- snapshotted at search time (logged-in users)
    contact_phone            VARCHAR(20),
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Detector job hot path: scan unsuppressed events with reminders_sent < 3
-- ordered by created_at to find candidates per pulse window.
CREATE INDEX IF NOT EXISTS idx_search_events_detector
    ON flights.flight_search_events (suppressed, reminders_sent, created_at);

-- Booking-side suppression check: when a booking happens, find matching
-- search event(s) by user + route + date and mark them BOOKED.
CREATE INDEX IF NOT EXISTS idx_search_events_user_route
    ON flights.flight_search_events (user_id, origin, destination, departure_date);

-- Anonymous identity promotion: when an anonymous user logs in, find
-- their device_id-keyed searches and attach the now-known user_id.
CREATE INDEX IF NOT EXISTS idx_search_events_device
    ON flights.flight_search_events (device_id);
