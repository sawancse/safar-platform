-- V30: Make grace period and penalty configurable with max cap + default grace to 5 days
ALTER TABLE bookings.pg_tenancies ADD COLUMN IF NOT EXISTS max_penalty_percent INT NOT NULL DEFAULT 25;

-- Update grace period default from 3 to 5 days (1st-5th of every month payment window)
ALTER TABLE bookings.pg_tenancies ALTER COLUMN grace_period_days SET DEFAULT 5;
UPDATE bookings.pg_tenancies SET grace_period_days = 5 WHERE grace_period_days = 3;

COMMENT ON COLUMN bookings.pg_tenancies.grace_period_days IS 'Days after due date before penalties start (default 5, payment window 1st-5th)';
COMMENT ON COLUMN bookings.pg_tenancies.late_penalty_bps IS 'Daily late penalty in basis points (100 = 1% per day)';
COMMENT ON COLUMN bookings.pg_tenancies.max_penalty_percent IS 'Maximum penalty as % of invoice total (0 = no cap, default 25%)';
