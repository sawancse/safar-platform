-- Razorpay linked account ID for host payouts via Razorpay Route
ALTER TABLE users.host_kyc ADD COLUMN IF NOT EXISTS razorpay_linked_account_id VARCHAR(100);
