-- Payout eligibility (checkout + dispute window) and batching of many settlement lines into one payout.
ALTER TABLE payments.settlement_lines ADD COLUMN IF NOT EXISTS payout_eligible_at TIMESTAMPTZ;

CREATE TABLE IF NOT EXISTS payments.payout_items (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payout_id          UUID NOT NULL REFERENCES payments.payouts(id),
    settlement_line_id UUID NOT NULL,
    booking_id         UUID,
    amount_paise       BIGINT NOT NULL CHECK (amount_paise > 0),
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- A settlement line can belong to at most one payout, ever.
CREATE UNIQUE INDEX IF NOT EXISTS uq_payout_items_line ON payments.payout_items(settlement_line_id);
CREATE INDEX IF NOT EXISTS idx_payout_items_payout ON payments.payout_items(payout_id);
