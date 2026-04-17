-- ============================================================
-- Professional Onboarding: Self-registration + Verification
-- ============================================================

-- Advocate onboarding fields
ALTER TABLE listings.advocates ADD COLUMN user_id UUID;
ALTER TABLE listings.advocates ADD COLUMN verification_status VARCHAR(20) DEFAULT 'APPROVED'; -- existing seed data = already approved
ALTER TABLE listings.advocates ADD COLUMN rejection_reason TEXT;
ALTER TABLE listings.advocates ADD COLUMN verified_by UUID;
ALTER TABLE listings.advocates ADD COLUMN verified_at TIMESTAMPTZ;
ALTER TABLE listings.advocates ADD COLUMN id_proof_url TEXT;
ALTER TABLE listings.advocates ADD COLUMN license_url TEXT;
ALTER TABLE listings.advocates ADD COLUMN certificate_urls TEXT;
ALTER TABLE listings.advocates ADD COLUMN languages TEXT;
ALTER TABLE listings.advocates ADD COLUMN available_days VARCHAR(50);
ALTER TABLE listings.advocates ADD COLUMN available_hours VARCHAR(30);

CREATE UNIQUE INDEX IF NOT EXISTS idx_advocates_user_id ON listings.advocates(user_id) WHERE user_id IS NOT NULL;

-- Interior Designer onboarding fields
ALTER TABLE listings.interior_designers ADD COLUMN user_id UUID;
ALTER TABLE listings.interior_designers ADD COLUMN verification_status VARCHAR(20) DEFAULT 'APPROVED';
ALTER TABLE listings.interior_designers ADD COLUMN rejection_reason TEXT;
ALTER TABLE listings.interior_designers ADD COLUMN verified_by UUID;
ALTER TABLE listings.interior_designers ADD COLUMN verified_at TIMESTAMPTZ;
ALTER TABLE listings.interior_designers ADD COLUMN id_proof_url TEXT;
ALTER TABLE listings.interior_designers ADD COLUMN license_url TEXT;
ALTER TABLE listings.interior_designers ADD COLUMN certificate_urls TEXT;
ALTER TABLE listings.interior_designers ADD COLUMN iiid_membership VARCHAR(50);
ALTER TABLE listings.interior_designers ADD COLUMN gst_number VARCHAR(20);
ALTER TABLE listings.interior_designers ADD COLUMN service_areas TEXT;
ALTER TABLE listings.interior_designers ADD COLUMN min_budget_paise BIGINT;

CREATE UNIQUE INDEX IF NOT EXISTS idx_designers_user_id ON listings.interior_designers(user_id) WHERE user_id IS NOT NULL;
