-- S27: Recurring Commercial Bookings
CREATE TABLE IF NOT EXISTS bookings.recurring_bookings (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    guest_id            UUID NOT NULL,
    listing_id          UUID NOT NULL,
    frequency           VARCHAR(20) NOT NULL,
    check_in_time       TIME NOT NULL,
    check_out_time      TIME NOT NULL,
    day_of_week         INT,
    start_date          DATE NOT NULL,
    end_date            DATE,
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    notice_days         INT NOT NULL DEFAULT 7,
    total_paid_paise    BIGINT NOT NULL DEFAULT 0,
    next_booking_date   DATE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_recurring_next ON bookings.recurring_bookings(next_booking_date, status);
