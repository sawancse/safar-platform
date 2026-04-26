-- Universal Trip ID — the cross-vertical container that owns flight + stay + cab
-- + cook + insurance bookings as a single logical "trip" identity.
--
-- Per Phase-2 Tree-3 of the TBO design brainstorm
-- (_bmad/docs/brainstorming/brainstorming-session-2026-04-26-tbo-design-plan.md):
-- This sits in booking-service rather than a new trip-service to avoid the
-- 12th microservice — booking-service already owns "bookings" and Trip is a
-- meta-booking that wraps multiple verticals.

CREATE TABLE IF NOT EXISTS bookings.trips (
    id                       UUID         PRIMARY KEY,
    user_id                  UUID         NOT NULL,
    trip_name                VARCHAR(200) NOT NULL,
    origin_city              VARCHAR(120),
    destination_city         VARCHAR(120),
    start_date               DATE,
    end_date                 DATE,
    trip_intent              VARCHAR(30)  NOT NULL DEFAULT 'UNCLASSIFIED',
    pax_count                INT          NOT NULL DEFAULT 1,
    status                   VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    bundle_discount_paise    BIGINT       NOT NULL DEFAULT 0,
    intent_overridden_by_user BOOLEAN     NOT NULL DEFAULT FALSE,
    notes                    TEXT,
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_trips_user_id        ON bookings.trips (user_id);
CREATE INDEX IF NOT EXISTS idx_trips_status         ON bookings.trips (status);
CREATE INDEX IF NOT EXISTS idx_trips_destination    ON bookings.trips (destination_city);
CREATE INDEX IF NOT EXISTS idx_trips_start_date     ON bookings.trips (start_date);

CREATE TABLE IF NOT EXISTS bookings.trip_legs (
    id                       UUID         PRIMARY KEY,
    trip_id                  UUID         NOT NULL REFERENCES bookings.trips(id) ON DELETE CASCADE,
    leg_type                 VARCHAR(20)  NOT NULL,
    -- references the booking row in the originating service.
    -- We don't FK across schemas/services; consistency enforced at app layer.
    external_booking_id      UUID         NOT NULL,
    external_service         VARCHAR(40)  NOT NULL,   -- 'flight-service' / 'listing-service' / 'chef-service' etc.
    status                   VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    leg_order                INT          NOT NULL DEFAULT 0,
    amount_paise             BIGINT,
    currency                 VARCHAR(3)   NOT NULL DEFAULT 'INR',
    refund_amount_paise      BIGINT,
    cancelled_at             TIMESTAMPTZ,
    cancellation_reason      TEXT,
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_trip_legs_trip_id    ON bookings.trip_legs (trip_id);
CREATE INDEX IF NOT EXISTS idx_trip_legs_external   ON bookings.trip_legs (external_service, external_booking_id);
CREATE INDEX IF NOT EXISTS idx_trip_legs_type       ON bookings.trip_legs (leg_type);
CREATE INDEX IF NOT EXISTS idx_trip_legs_status     ON bookings.trip_legs (status);

-- Prevent same external booking being attached to two trips.
CREATE UNIQUE INDEX IF NOT EXISTS uniq_trip_legs_external_booking
    ON bookings.trip_legs (external_service, external_booking_id);
