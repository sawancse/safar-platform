-- V22: Assignment join between an event_booking (a bespoke order) and
-- the partner_vendor that fulfils it. Mirrors event_booking_staff in
-- spirit but is for external vendor partners, not chef-team staff.
--
-- Status lifecycle:  ASSIGNED -> CONFIRMED -> DELIVERED
--                              \-> CANCELLED (any time)
--
-- Only one active assignment per booking is allowed (partial unique index
-- excludes CANCELLED rows, so a booking can be reassigned after cancel).

CREATE TABLE IF NOT EXISTS chefs.event_booking_vendor (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_booking_id  UUID         NOT NULL,
    vendor_id         UUID         NOT NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT 'ASSIGNED',  -- ASSIGNED / CONFIRMED / DELIVERED / CANCELLED
    assigned_at       TIMESTAMPTZ  DEFAULT now(),
    confirmed_at      TIMESTAMPTZ,
    delivered_at      TIMESTAMPTZ,
    cancelled_at      TIMESTAMPTZ,
    cancel_reason     TEXT,
    payout_paise      BIGINT,                  -- what the platform owes the vendor
    payout_status     VARCHAR(20)  DEFAULT 'PENDING',  -- PENDING / PAID
    payout_ref        VARCHAR(60),             -- NEFT UTR / Razorpay payout id
    payout_at         TIMESTAMPTZ,
    admin_notes       TEXT,
    created_at        TIMESTAMPTZ  DEFAULT now(),
    updated_at        TIMESTAMPTZ  DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_ebv_booking ON chefs.event_booking_vendor (event_booking_id);
CREATE INDEX IF NOT EXISTS idx_ebv_vendor  ON chefs.event_booking_vendor (vendor_id);
CREATE INDEX IF NOT EXISTS idx_ebv_status  ON chefs.event_booking_vendor (status);

-- Enforce single active vendor per booking (allows re-assignment after cancel).
CREATE UNIQUE INDEX IF NOT EXISTS uk_ebv_booking_active
  ON chefs.event_booking_vendor (event_booking_id)
  WHERE status <> 'CANCELLED';
