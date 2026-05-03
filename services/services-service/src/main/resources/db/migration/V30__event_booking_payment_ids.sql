-- ============================================================================
-- V30: Capture Razorpay order/payment IDs on event_bookings
-- ============================================================================
-- Until now event_bookings.balance_paid_at was flipped server-side without
-- ever recording the Razorpay handles, which meant:
--   1. We had no audit trail to reconcile against Razorpay statements
--   2. Anyone with a customer JWT could call POST /chef-events/{id}/pay-balance
--      directly and have us mark balance paid without any actual transfer
--
-- These columns let payBalance() require the Razorpay orderId + paymentId on
-- the call, store them for reconciliation, and reject calls missing them.
-- We add advance columns as well so the same hardening can be applied to
-- markAdvancePaid in a follow-up pass without another migration.
-- ============================================================================

ALTER TABLE chefs.event_bookings
    ADD COLUMN IF NOT EXISTS advance_razorpay_order_id   VARCHAR(64),
    ADD COLUMN IF NOT EXISTS advance_razorpay_payment_id VARCHAR(64),
    ADD COLUMN IF NOT EXISTS balance_razorpay_order_id   VARCHAR(64),
    ADD COLUMN IF NOT EXISTS balance_razorpay_payment_id VARCHAR(64);

-- chef_bookings already has razorpay_order_id / razorpay_payment_id from V1
-- (those captured advance handles via confirmPayment). Add balance-specific
-- columns so payBalance can record the second transaction without overwriting
-- the advance audit trail. Existing columns stay for advance, new columns for
-- balance — symmetrical with event_bookings.
ALTER TABLE chefs.chef_bookings
    ADD COLUMN IF NOT EXISTS balance_razorpay_order_id   VARCHAR(64),
    ADD COLUMN IF NOT EXISTS balance_razorpay_payment_id VARCHAR(64);
