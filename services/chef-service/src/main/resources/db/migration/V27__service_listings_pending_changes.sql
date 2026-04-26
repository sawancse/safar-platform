-- V27: Re-review for post-VERIFIED edits — pending changes overlay.
--
-- When a VERIFIED vendor edits a material field (price formula, business
-- name, pricing pattern), the change goes into pending_changes JSONB and
-- has_pending_changes flips to true. The current values stay live + bookable
-- while admin reviews — Airbnb / Etsy pattern, vendor doesn't go offline
-- during a price tweak.
--
-- Non-material edits (tagline, photos, about_md, type-attribute fields) are
-- applied silently — vendors don't wait on admin to add a new cake flavour
-- to their listing. Material vs non-material is enforced in code
-- (ServiceListingService.MATERIAL_FIELDS).
--
-- Admin actions:
--   approve-changes  → merge pending_changes onto entity, clear flag
--   reject-changes   → clear pending_changes (no entity change), clear flag
--
-- pending_changes_submitted_at lets ops sort the queue by oldest pending
-- first — same SLA promise as new-listing review (24 hr p95).

ALTER TABLE services.service_listings
    ADD COLUMN IF NOT EXISTS has_pending_changes          BOOLEAN     NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS pending_changes              JSONB,
    ADD COLUMN IF NOT EXISTS pending_changes_submitted_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_listings_pending_changes
    ON services.service_listings(pending_changes_submitted_at)
    WHERE has_pending_changes = TRUE;
