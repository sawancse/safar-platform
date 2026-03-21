-- Add scheduling and retry columns to payouts table
ALTER TABLE payments.payouts ADD COLUMN IF NOT EXISTS scheduled_batch VARCHAR(30);
ALTER TABLE payments.payouts ADD COLUMN IF NOT EXISTS scheduled_at TIMESTAMPTZ;
ALTER TABLE payments.payouts ADD COLUMN IF NOT EXISTS retry_count INTEGER DEFAULT 0;
ALTER TABLE payments.payouts ADD COLUMN IF NOT EXISTS failure_reason VARCHAR(500);

CREATE INDEX IF NOT EXISTS idx_payouts_status_scheduled ON payments.payouts(status, scheduled_at);
CREATE INDEX IF NOT EXISTS idx_payouts_host_status ON payments.payouts(host_id, status);
