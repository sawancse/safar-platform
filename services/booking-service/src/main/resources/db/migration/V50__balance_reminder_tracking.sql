-- V50: Track balance-payment reminder sends for PARTIAL_PREPAID bookings.
-- Prevents the daily scheduler from re-sending the same reminder.
ALTER TABLE bookings.bookings
    ADD COLUMN IF NOT EXISTS balance_reminder_t7_sent_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS balance_reminder_t1_sent_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX IF NOT EXISTS idx_bookings_balance_reminder
    ON bookings.bookings (payment_mode, status, check_in)
    WHERE payment_mode = 'PARTIAL_PREPAID' AND due_at_property_paise > 0;
