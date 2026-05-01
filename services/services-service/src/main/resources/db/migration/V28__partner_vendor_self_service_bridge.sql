-- ============================================================================
-- V28: Bridge self-service ServiceListing vendors into the PartnerVendor model
-- ============================================================================
-- Until now, partner_vendors was admin-only (no user_id) while service_listings
-- was the self-service vendor model (has vendor_user_id). This split meant
-- self-service vendors had nowhere to receive booking assignments.
--
-- This migration adds nullable user_id + service_listing_id to partner_vendors
-- so a stub PartnerVendor can be auto-created when a ServiceListing is approved
-- (see ServiceListingService.approve). All booking-vendor joins continue to
-- target partner_vendors.id — the canonical vendor row — but self-service
-- vendors now appear there too, so vendor-side queries can find their work.
--
-- Phone is relaxed from NOT NULL → nullable: stub rows don't have phone yet
-- (we can fetch from user-service on demand). Existing admin-onboarded rows
-- are unaffected.
-- ============================================================================

ALTER TABLE chefs.partner_vendors
    ADD COLUMN IF NOT EXISTS user_id UUID,
    ADD COLUMN IF NOT EXISTS service_listing_id UUID
        REFERENCES services.service_listings(id) ON DELETE SET NULL;

ALTER TABLE chefs.partner_vendors
    ALTER COLUMN phone DROP NOT NULL;

CREATE INDEX IF NOT EXISTS idx_partner_vendors_user_id
    ON chefs.partner_vendors(user_id) WHERE user_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_partner_vendors_service_listing_id
    ON chefs.partner_vendors(service_listing_id) WHERE service_listing_id IS NOT NULL;
