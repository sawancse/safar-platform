-- Backfill: deposits that were collected as part of a fully-paid booking but were
-- left stuck at PENDING (status was never advanced on payment success before this fix).
-- A deposit is collected when the booking is paid and nothing is still due at property.
UPDATE bookings.bookings
SET security_deposit_status = 'COLLECTED'
WHERE security_deposit_status = 'PENDING'
  AND security_deposit_paise IS NOT NULL
  AND security_deposit_paise > 0
  AND status IN ('CONFIRMED', 'COMPLETED')
  AND (due_at_property_paise IS NULL OR due_at_property_paise = 0);
