-- V59: Configurable deposit, maintenance, and lease for all listing types
ALTER TABLE listings.listings ADD COLUMN IF NOT EXISTS deposit_type VARCHAR(20) DEFAULT 'REFUNDABLE';
ALTER TABLE listings.listings ADD COLUMN IF NOT EXISTS deposit_terms TEXT;
ALTER TABLE listings.listings ADD COLUMN IF NOT EXISTS maintenance_charge_paise BIGINT;
ALTER TABLE listings.listings ADD COLUMN IF NOT EXISTS min_lease_months INT;
