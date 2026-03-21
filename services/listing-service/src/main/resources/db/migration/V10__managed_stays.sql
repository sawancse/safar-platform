CREATE TABLE listings.managed_stay_contracts (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id          UUID NOT NULL,
    host_id             UUID NOT NULL,
    management_fee_pct  INT NOT NULL DEFAULT 18,
    contract_start      DATE NOT NULL,
    contract_end        DATE,
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    auto_pricing        BOOLEAN NOT NULL DEFAULT true,
    auto_cleaning       BOOLEAN NOT NULL DEFAULT true,
    guest_screening     BOOLEAN NOT NULL DEFAULT true,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE listings.managed_stay_expenses (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contract_id     UUID NOT NULL,
    booking_id      UUID,
    expense_type    VARCHAR(50) NOT NULL,
    amount_paise    BIGINT NOT NULL,
    description     VARCHAR(255),
    receipt_url     VARCHAR(500),
    incurred_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE listings.managed_stay_payouts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contract_id     UUID NOT NULL,
    period_start    DATE NOT NULL,
    period_end      DATE NOT NULL,
    gross_revenue   BIGINT NOT NULL,
    expenses        BIGINT NOT NULL DEFAULT 0,
    management_fee  BIGINT NOT NULL,
    net_payout      BIGINT NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payout_date     DATE
);
