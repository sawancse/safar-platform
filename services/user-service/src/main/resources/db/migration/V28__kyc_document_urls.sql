-- V28: Add document upload URLs to host_kyc for identity verification
ALTER TABLE users.host_kyc ADD COLUMN IF NOT EXISTS aadhaar_front_url VARCHAR(500);
ALTER TABLE users.host_kyc ADD COLUMN IF NOT EXISTS aadhaar_back_url VARCHAR(500);
ALTER TABLE users.host_kyc ADD COLUMN IF NOT EXISTS pan_url VARCHAR(500);
ALTER TABLE users.host_kyc ADD COLUMN IF NOT EXISTS selfie_url VARCHAR(500);
