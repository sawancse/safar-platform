-- V21: Medical procedures and hospital enhancements
CREATE TABLE IF NOT EXISTS listings.hospital_procedures (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hospital_id UUID NOT NULL,
    procedure_name VARCHAR(200) NOT NULL,
    specialty VARCHAR(100) NOT NULL,
    est_cost_min_paise BIGINT NOT NULL,
    est_cost_max_paise BIGINT NOT NULL,
    hospital_days INT NOT NULL DEFAULT 3,
    recovery_days INT NOT NULL DEFAULT 7,
    success_rate NUMERIC(5,2),
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_hospital_procedures_hospital ON listings.hospital_procedures (hospital_id);
CREATE INDEX IF NOT EXISTS idx_hospital_procedures_specialty ON listings.hospital_procedures (specialty);

-- Enhance hospital_partners
ALTER TABLE listings.hospital_partners ADD COLUMN IF NOT EXISTS photos TEXT[] DEFAULT '{}';
ALTER TABLE listings.hospital_partners ADD COLUMN IF NOT EXISTS airport_distance_km NUMERIC(5,1);
ALTER TABLE listings.hospital_partners ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE listings.hospital_partners ADD COLUMN IF NOT EXISTS phone VARCHAR(20);
ALTER TABLE listings.hospital_partners ADD COLUMN IF NOT EXISTS website VARCHAR(255);
ALTER TABLE listings.hospital_partners ADD COLUMN IF NOT EXISTS rating NUMERIC(3,1);

-- Enhance medical_stay_packages
ALTER TABLE listings.medical_stay_packages ADD COLUMN IF NOT EXISTS recovery_days INT DEFAULT 7;
