CREATE TABLE users.cohost_profiles (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    host_id             UUID NOT NULL UNIQUE,
    bio                 TEXT,
    services_offered    TEXT NOT NULL DEFAULT '',
    cities              TEXT NOT NULL DEFAULT '',
    min_fee_pct         INT NOT NULL DEFAULT 5,
    max_fee_pct         INT NOT NULL DEFAULT 15,
    max_listings        INT DEFAULT 5,
    current_listings    INT NOT NULL DEFAULT 0,
    rating              NUMERIC(3,2),
    review_count        INT NOT NULL DEFAULT 0,
    verified            BOOLEAN NOT NULL DEFAULT false,
    active              BOOLEAN NOT NULL DEFAULT true,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE users.cohost_agreements (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id      UUID NOT NULL,
    host_id         UUID NOT NULL,
    cohost_id       UUID NOT NULL,
    fee_pct         INT NOT NULL,
    services        TEXT NOT NULL DEFAULT '',
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    start_date      DATE NOT NULL,
    end_date        DATE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE users.cohost_earnings (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agreement_id    UUID NOT NULL,
    booking_id      UUID NOT NULL,
    amount_paise    BIGINT NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    paid_at         TIMESTAMPTZ
);
