CREATE TABLE listings.experiences (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    host_id         UUID NOT NULL,
    title           VARCHAR(200) NOT NULL,
    description     TEXT NOT NULL,
    category        VARCHAR(50) NOT NULL,
    city            VARCHAR(100) NOT NULL,
    location_name   VARCHAR(200),
    duration_hours  NUMERIC(4,1) NOT NULL,
    max_guests      INT NOT NULL DEFAULT 10,
    price_paise     BIGINT NOT NULL,
    languages_spoken TEXT NOT NULL DEFAULT 'en',
    media_urls      TEXT NOT NULL DEFAULT '',
    status          VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    rating          NUMERIC(3,2),
    review_count    INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE listings.experience_sessions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    experience_id   UUID NOT NULL,
    session_date    DATE NOT NULL,
    start_time      TIME NOT NULL,
    end_time        TIME NOT NULL,
    available_spots INT NOT NULL,
    booked_spots    INT NOT NULL DEFAULT 0,
    status          VARCHAR(20) NOT NULL DEFAULT 'OPEN'
);

CREATE TABLE listings.experience_bookings (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    experience_id   UUID NOT NULL,
    session_id      UUID NOT NULL,
    guest_id        UUID NOT NULL,
    property_booking_id UUID,
    num_guests      INT NOT NULL DEFAULT 1,
    total_paise     BIGINT NOT NULL,
    platform_fee_paise BIGINT NOT NULL,
    host_payout_paise  BIGINT NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED',
    ref             VARCHAR(20) NOT NULL UNIQUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
