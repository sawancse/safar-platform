-- Link payouts to settlement lines (idempotency) and allow REVERSED/CANCELLED statuses.
-- V1 status CHECK omitted REVERSED, so the payout.reversed webhook could not be persisted.
ALTER TABLE payments.payouts ADD COLUMN IF NOT EXISTS settlement_line_id UUID;
ALTER TABLE payments.payouts ADD COLUMN IF NOT EXISTS fund_account_id VARCHAR(40);
ALTER TABLE payments.payouts ADD COLUMN IF NOT EXISTS utr VARCHAR(40);
ALTER TABLE payments.payouts ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(40);

CREATE UNIQUE INDEX IF NOT EXISTS uq_payouts_settlement_line
    ON payments.payouts(settlement_line_id) WHERE settlement_line_id IS NOT NULL;

ALTER TABLE payments.payouts DROP CONSTRAINT IF EXISTS payouts_status_check;
ALTER TABLE payments.payouts ADD CONSTRAINT payouts_status_check
    CHECK (status IN ('PENDING','PROCESSING','COMPLETED','FAILED','REVERSED','CANCELLED'));
