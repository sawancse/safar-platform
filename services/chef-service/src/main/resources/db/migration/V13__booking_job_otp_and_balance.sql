-- Start-job OTP + balance-payment tracking for both booking types.
-- Customer sees the OTP on their booking page; chef types it in to start the job.

ALTER TABLE chefs.chef_bookings
    ADD COLUMN IF NOT EXISTS start_job_otp VARCHAR(6),
    ADD COLUMN IF NOT EXISTS job_started_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS balance_paid_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS appliances_json TEXT,
    ADD COLUMN IF NOT EXISTS crockery_json TEXT;

ALTER TABLE chefs.event_bookings
    ADD COLUMN IF NOT EXISTS start_job_otp VARCHAR(6),
    ADD COLUMN IF NOT EXISTS job_started_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS balance_paid_at TIMESTAMPTZ;

-- Backfill OTPs for existing CONFIRMED bookings so in-flight jobs can still be started.
UPDATE chefs.chef_bookings
SET start_job_otp = LPAD((FLOOR(RANDOM() * 10000))::TEXT, 4, '0')
WHERE start_job_otp IS NULL
  AND status IN ('CONFIRMED', 'IN_PROGRESS');

UPDATE chefs.event_bookings
SET start_job_otp = LPAD((FLOOR(RANDOM() * 10000))::TEXT, 4, '0')
WHERE start_job_otp IS NULL
  AND status IN ('CONFIRMED', 'IN_PROGRESS');
