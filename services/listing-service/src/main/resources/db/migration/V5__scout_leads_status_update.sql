-- V5: Update listing_drafts status check to include CONVERTED
ALTER TABLE listings.listing_drafts DROP CONSTRAINT IF EXISTS listing_drafts_status_check;
ALTER TABLE listings.listing_drafts ADD CONSTRAINT listing_drafts_status_check
    CHECK (status IN ('DRAFT','APPROVED','CONVERTED'));
