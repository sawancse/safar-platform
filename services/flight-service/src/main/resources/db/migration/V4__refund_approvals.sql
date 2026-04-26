-- Two-step admin refund approval queue, per Tree-4 of the TBO design.
-- Refunds ≤ ₹10k auto-confirm; >₹10k OR partial-fare go to this queue
-- for L1 admin review (4hr SLA for >₹50k via priority sort).

CREATE TABLE IF NOT EXISTS flights.refund_approvals (
    id                      UUID         PRIMARY KEY,
    flight_booking_id       UUID         NOT NULL,
    user_id                 UUID         NOT NULL,
    requested_amount_paise  BIGINT       NOT NULL,
    approved_amount_paise   BIGINT,
    status                  VARCHAR(20)  NOT NULL DEFAULT 'PENDING',  -- PENDING / APPROVED / REJECTED / COMPLETED
    priority                VARCHAR(10)  NOT NULL DEFAULT 'NORMAL',    -- NORMAL / HIGH (>₹50k or intl or group)
    reason                  TEXT,                                       -- user-supplied cancellation reason
    fare_rule               VARCHAR(20),                                -- REFUNDABLE / PARTIAL / NON_REFUNDABLE
    requested_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    reviewed_at             TIMESTAMPTZ,
    reviewed_by_user_id     UUID,
    review_notes            TEXT,
    completed_at            TIMESTAMPTZ,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_refund_approvals_status_priority
    ON flights.refund_approvals (status, priority, requested_at);
CREATE INDEX IF NOT EXISTS idx_refund_approvals_booking
    ON flights.refund_approvals (flight_booking_id);
CREATE INDEX IF NOT EXISTS idx_refund_approvals_user
    ON flights.refund_approvals (user_id);
