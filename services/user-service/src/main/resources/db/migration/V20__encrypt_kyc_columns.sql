-- Widen KYC columns to accommodate AES-256-GCM encrypted ciphertext (Base64 encoded)
-- Plaintext 12 chars (Aadhaar) → ~60 chars ciphertext; 20 chars (bank) → ~72 chars
-- Using TEXT for safety — no length constraints on encrypted data

ALTER TABLE users.host_kyc ALTER COLUMN aadhaar_number TYPE TEXT;
ALTER TABLE users.host_kyc ALTER COLUMN pan_number TYPE TEXT;
ALTER TABLE users.host_kyc ALTER COLUMN bank_account_number TYPE TEXT;
ALTER TABLE users.host_kyc ALTER COLUMN bank_ifsc TYPE TEXT;
