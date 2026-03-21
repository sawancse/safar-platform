CREATE SCHEMA IF NOT EXISTS bookings;

CREATE TABLE bookings.bookings (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_ref             VARCHAR(20) UNIQUE NOT NULL,
    guest_id                UUID NOT NULL,
    host_id                 UUID NOT NULL,
    listing_id              UUID NOT NULL,
    check_in                TIMESTAMPTZ NOT NULL,
    check_out               TIMESTAMPTZ NOT NULL,
    guests_count            INTEGER NOT NULL,
    nights                  INTEGER,
    hours                   DECIMAL(4,2),
    status                  VARCHAR(20) DEFAULT 'DRAFT'
                                CHECK (status IN ('DRAFT','PENDING_PAYMENT','CONFIRMED',
                                                  'CHECKED_IN','COMPLETED','CANCELLED','NO_SHOW')),
    base_amount_paise       BIGINT NOT NULL,
    insurance_amount_paise  BIGINT NOT NULL,
    gst_amount_paise        BIGINT NOT NULL,
    total_amount_paise      BIGINT NOT NULL,
    host_payout_paise       BIGINT NOT NULL,
    cancellation_reason     TEXT,
    cancelled_at            TIMESTAMPTZ,
    checked_in_at           TIMESTAMPTZ,
    completed_at            TIMESTAMPTZ,
    august_code             VARCHAR(50),
    is_recurring            BOOLEAN DEFAULT FALSE,
    recurring_id            UUID,
    created_at              TIMESTAMPTZ DEFAULT NOW(),
    updated_at              TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE bookings.stay_feedback (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id      UUID NOT NULL REFERENCES bookings.bookings(id),
    guest_id        UUID NOT NULL,
    pulse_number    INTEGER NOT NULL,
    overall_rating  SMALLINT CHECK (overall_rating BETWEEN 1 AND 5),
    cleanliness     SMALLINT CHECK (cleanliness BETWEEN 1 AND 5),
    wifi_rating     SMALLINT CHECK (wifi_rating BETWEEN 1 AND 5),
    accuracy        SMALLINT CHECK (accuracy BETWEEN 1 AND 5),
    noise_rating    SMALLINT CHECK (noise_rating BETWEEN 1 AND 5),
    safety_rating   SMALLINT CHECK (safety_rating BETWEEN 1 AND 5),
    comment         TEXT,
    issue_raised    BOOLEAN DEFAULT FALSE,
    issue_resolved  BOOLEAN DEFAULT FALSE,
    compensation_triggered BOOLEAN DEFAULT FALSE,
    compensation_type VARCHAR(20),
    compensation_amount_paise BIGINT,
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_bookings_guest   ON bookings.bookings(guest_id);
CREATE INDEX idx_bookings_host    ON bookings.bookings(host_id);
CREATE INDEX idx_bookings_listing ON bookings.bookings(listing_id);
CREATE INDEX idx_bookings_status  ON bookings.bookings(status);
