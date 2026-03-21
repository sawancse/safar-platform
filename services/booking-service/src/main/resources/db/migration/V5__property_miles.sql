-- S23: Property Miles Loyalty Program
CREATE TABLE IF NOT EXISTS bookings.miles_balance (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL UNIQUE,
    balance     BIGINT NOT NULL DEFAULT 0,
    lifetime    BIGINT NOT NULL DEFAULT 0,
    tier        VARCHAR(20) NOT NULL DEFAULT 'BRONZE',
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS bookings.miles_ledger (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL,
    booking_id      UUID,
    transaction_type VARCHAR(30) NOT NULL,
    amount          BIGINT NOT NULL,
    balance_after   BIGINT NOT NULL,
    description     VARCHAR(255),
    expires_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_miles_ledger_user ON bookings.miles_ledger(user_id, created_at DESC);
