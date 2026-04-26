-- V26: Vendor invites — Pattern E (Zomato WhatsApp BD outreach).
--
-- BD agent generates a unique invite token for a prospective vendor → sends
-- via WhatsApp (manual cut-paste in V1; templated MSG91 in V2). Vendor taps
-- /vendor/onboard/{type}?invite={token} → wizard pre-fills phone + OTP-skip
-- (since the invite proves we initiated the conversation) → vendor onboards
-- without creating a fresh account.
--
-- The invite row is also our funnel telemetry: how many sent, how many
-- opened the link, how many submitted, how many got approved.

CREATE TABLE IF NOT EXISTS services.vendor_invites (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invite_token        VARCHAR(64)  NOT NULL UNIQUE,    -- random 32-char hex
    phone               VARCHAR(20)  NOT NULL,           -- in E.164 (e.g. +919998887776)
    business_name       VARCHAR(200),                    -- BD agent's guess; vendor can edit during wizard
    service_type        VARCHAR(40)  NOT NULL,           -- ServiceListingType
    notes               TEXT,                            -- "referred by Bakery X", "spotted at wedding expo"

    -- Funnel telemetry — every state transition stamped
    sent_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    opened_at           TIMESTAMPTZ,                     -- vendor tapped the link
    onboarding_started_at TIMESTAMPTZ,                   -- vendor advanced past step 1
    submitted_at        TIMESTAMPTZ,                     -- vendor hit "Submit" (DRAFT → PENDING_REVIEW)
    completed_at        TIMESTAMPTZ,                     -- admin approved (PENDING_REVIEW → VERIFIED)
    expired_at          TIMESTAMPTZ,                     -- 30-day TTL hits
    cancelled_at        TIMESTAMPTZ,                     -- BD agent cancels before vendor opens

    -- Channel + ownership
    sent_via            VARCHAR(20)  NOT NULL DEFAULT 'MANUAL',  -- MANUAL | MSG91 | OTHER
    sent_by             UUID,                            -- BD agent (admin user id)
    service_listing_id  UUID,                            -- populated when vendor onboards via this invite

    expires_at          TIMESTAMPTZ  NOT NULL DEFAULT (now() + INTERVAL '30 days'),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT chk_vendor_invites_sent_via CHECK (sent_via IN ('MANUAL','MSG91','OTHER'))
);

CREATE INDEX IF NOT EXISTS idx_vendor_invites_token ON services.vendor_invites(invite_token);
CREATE INDEX IF NOT EXISTS idx_vendor_invites_phone ON services.vendor_invites(phone);
CREATE INDEX IF NOT EXISTS idx_vendor_invites_funnel ON services.vendor_invites(sent_at DESC);
CREATE INDEX IF NOT EXISTS idx_vendor_invites_open ON services.vendor_invites(opened_at) WHERE opened_at IS NOT NULL;
