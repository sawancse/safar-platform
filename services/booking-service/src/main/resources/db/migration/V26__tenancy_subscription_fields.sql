-- Add subscription and penalty fields to PG tenancies and invoices
ALTER TABLE bookings.pg_tenancies ADD COLUMN IF NOT EXISTS razorpay_plan_id VARCHAR(100);
ALTER TABLE bookings.pg_tenancies ADD COLUMN IF NOT EXISTS subscription_status VARCHAR(20);
ALTER TABLE bookings.pg_tenancies ADD COLUMN IF NOT EXISTS grace_period_days INT NOT NULL DEFAULT 3;
ALTER TABLE bookings.pg_tenancies ADD COLUMN IF NOT EXISTS late_penalty_bps INT NOT NULL DEFAULT 200;

ALTER TABLE bookings.tenancy_invoices ADD COLUMN IF NOT EXISTS razorpay_subscription_id VARCHAR(100);
ALTER TABLE bookings.tenancy_invoices ADD COLUMN IF NOT EXISTS late_penalty_paise BIGINT NOT NULL DEFAULT 0;
ALTER TABLE bookings.tenancy_invoices ADD COLUMN IF NOT EXISTS water_paise BIGINT NOT NULL DEFAULT 0;
