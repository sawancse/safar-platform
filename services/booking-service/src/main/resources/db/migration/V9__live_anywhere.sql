CREATE TABLE IF NOT EXISTS bookings.live_anywhere_subscriptions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    guest_id            UUID NOT NULL UNIQUE,
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    nights_used_this_month INT NOT NULL DEFAULT 0,
    max_nights_per_month   INT NOT NULL DEFAULT 30,
    max_covered_paise      BIGINT NOT NULL DEFAULT 300000,
    current_stay_id        UUID,
    started_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    next_billing_date   DATE NOT NULL,
    cancelled_at        TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS bookings.live_anywhere_stays (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subscription_id UUID NOT NULL,
    booking_id      UUID NOT NULL,
    check_in        DATE NOT NULL,
    check_out       DATE NOT NULL,
    nights          INT NOT NULL,
    listing_price   BIGINT NOT NULL,
    covered_paise   BIGINT NOT NULL,
    guest_topup     BIGINT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
