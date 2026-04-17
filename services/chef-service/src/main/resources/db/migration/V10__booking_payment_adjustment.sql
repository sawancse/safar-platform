-- Track actual paid amounts separately from required amounts on booking modification
ALTER TABLE chefs.chef_bookings
    ADD COLUMN IF NOT EXISTS advance_paid_paise BIGINT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS payment_adjustment_paise BIGINT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS previous_total_paise BIGINT;

-- Backfill: if advance was already paid, set advance_paid = advance_amount
UPDATE chefs.chef_bookings
SET advance_paid_paise = advance_amount_paise
WHERE payment_status IN ('ADVANCE_PAID', 'PAID', 'FULLY_PAID')
  AND advance_paid_paise = 0;

COMMENT ON COLUMN chefs.chef_bookings.advance_paid_paise IS 'Actual amount paid as advance (does not change on modify)';
COMMENT ON COLUMN chefs.chef_bookings.payment_adjustment_paise IS 'Positive = user owes more, Negative = user is owed a refund, Zero = no change';
COMMENT ON COLUMN chefs.chef_bookings.previous_total_paise IS 'Total before last modification (for audit trail)';
