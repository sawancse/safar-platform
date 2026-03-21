CREATE TABLE IF NOT EXISTS bookings.cleaner_profiles (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL,
    full_name       VARCHAR(100) NOT NULL,
    phone           VARCHAR(15) NOT NULL,
    cities          TEXT NOT NULL DEFAULT '',
    rate_per_hour_paise BIGINT NOT NULL,
    rating          NUMERIC(3,2),
    job_count       INT NOT NULL DEFAULT 0,
    verified        BOOLEAN NOT NULL DEFAULT false,
    available       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS bookings.cleaning_jobs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id      UUID NOT NULL,
    booking_id      UUID,
    cleaner_id      UUID,
    scheduled_at    TIMESTAMPTZ NOT NULL,
    estimated_hours NUMERIC(3,1) NOT NULL DEFAULT 2.0,
    status          VARCHAR(20) NOT NULL DEFAULT 'UNASSIGNED',
    completed_at    TIMESTAMPTZ,
    amount_paise    BIGINT,
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
