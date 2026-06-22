-- V85: Aadhaar eSign + government e-Stamp integration columns for agreements.
-- Reuses existing columns: agreement_requests.e_stamp_id (stamp cert no),
-- signed_pdf_url (final signed PDF), stamp_duty_paise; agreement_parties.aadhaar_number,
-- e_sign_status, e_sign_request_id, signed_at. Adds provider/audit fields below.

ALTER TABLE listings.agreement_requests
    ADD COLUMN IF NOT EXISTS esign_provider     VARCHAR(30),
    ADD COLUMN IF NOT EXISTS esign_document_id  VARCHAR(120),   -- provider envelope/document id
    ADD COLUMN IF NOT EXISTS esign_status       VARCHAR(30),    -- NONE/INITIATED/PARTIALLY_SIGNED/SIGNED/FAILED
    ADD COLUMN IF NOT EXISTS unsigned_pdf_url    TEXT,           -- persisted pre-sign artifact (hashable)
    ADD COLUMN IF NOT EXISTS estamp_provider     VARCHAR(30),
    ADD COLUMN IF NOT EXISTS estamp_pdf_url       TEXT,          -- e-stamp certificate PDF
    ADD COLUMN IF NOT EXISTS estamp_issued_at     TIMESTAMPTZ;

ALTER TABLE listings.agreement_parties
    ADD COLUMN IF NOT EXISTS esign_signing_url      TEXT,        -- per-party Aadhaar eSign link
    ADD COLUMN IF NOT EXISTS esign_signer_vid_masked VARCHAR(20); -- masked VID/Aadhaar returned by ESP (never store full Aadhaar)
