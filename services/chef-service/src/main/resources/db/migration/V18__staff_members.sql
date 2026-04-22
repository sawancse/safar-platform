-- V18: Chef-owned service staff roster + per-booking assignments.
-- Phase A of the "Service Staff" feature (Option 3): chefs manage their
-- own team; a later migration can flip chef_id nullable for the
-- admin-managed platform pool.

CREATE TABLE IF NOT EXISTS chefs.staff_members (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chef_id            UUID NOT NULL,
    name               VARCHAR(120) NOT NULL,
    role               VARCHAR(30)  NOT NULL,   -- item_key from STAFF_ROLE pricing (waiter/cleaner/bartender/...)
    phone              VARCHAR(20),
    photo_url          VARCHAR(500),
    kyc_status         VARCHAR(20)  DEFAULT 'PENDING',  -- PENDING / VERIFIED / REJECTED
    hourly_rate_paise  BIGINT,                  -- optional per-person override; NULL = use chef/default rate
    languages          VARCHAR(200),            -- comma-separated, e.g. "Hindi,English,Tamil"
    years_experience   INT,
    notes              TEXT,
    active             BOOLEAN      DEFAULT TRUE,
    created_at         TIMESTAMPTZ  DEFAULT now(),
    updated_at         TIMESTAMPTZ  DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_staff_members_chef        ON chefs.staff_members(chef_id);
CREATE INDEX IF NOT EXISTS idx_staff_members_chef_active ON chefs.staff_members(chef_id, active);
CREATE INDEX IF NOT EXISTS idx_staff_members_role        ON chefs.staff_members(role);

-- Assignment of staff to a specific event booking.
-- One row per (booking, staff). Chef can reassign by deleting + reinserting.
CREATE TABLE IF NOT EXISTS chefs.event_booking_staff (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id      UUID NOT NULL,
    staff_id        UUID NOT NULL,
    role            VARCHAR(30) NOT NULL,   -- denormalised from staff_members.role at assignment time
    rate_paise      BIGINT      NOT NULL,   -- locked rate for this booking (protects against future rate changes)
    assigned_at     TIMESTAMPTZ DEFAULT now(),
    check_in_at     TIMESTAMPTZ,            -- filled when customer hands OTP on event day (Phase C)
    check_in_otp    VARCHAR(6),             -- 4-6 digit OTP, generated when booking is confirmed
    rating          SMALLINT,               -- customer rating 1-5 (Phase C)
    no_show         BOOLEAN     DEFAULT FALSE,
    created_at      TIMESTAMPTZ DEFAULT now(),
    CONSTRAINT uk_event_booking_staff UNIQUE (booking_id, staff_id)
);

CREATE INDEX IF NOT EXISTS idx_ebs_booking ON chefs.event_booking_staff(booking_id);
CREATE INDEX IF NOT EXISTS idx_ebs_staff   ON chefs.event_booking_staff(staff_id);
