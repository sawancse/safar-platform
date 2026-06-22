-- V2: link a policy to a booking (embedded insurance) — nullable so standalone
-- marketplace policies (no booking) work the same way.
ALTER TABLE insurance.insurance_policies
    ADD COLUMN IF NOT EXISTS booking_id UUID;

CREATE INDEX IF NOT EXISTS idx_insurance_booking ON insurance.insurance_policies (booking_id) WHERE booking_id IS NOT NULL;
