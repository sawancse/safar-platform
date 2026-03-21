CREATE TABLE users.nomad_prime_memberships (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    guest_id              UUID NOT NULL UNIQUE,
    status                VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    discount_pct          INT NOT NULL DEFAULT 15,
    monthly_bonus_miles   INT NOT NULL DEFAULT 500,
    insurance_cover_paise BIGINT NOT NULL DEFAULT 50000000,
    started_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    next_renewal_date     DATE NOT NULL,
    cancelled_at          TIMESTAMPTZ
);
