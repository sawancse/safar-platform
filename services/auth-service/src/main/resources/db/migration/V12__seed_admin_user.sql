-- V12: seed the bootstrap admin so first-ever login succeeds.
-- V9 only UPDATE-d the role, so if +917367034295 had never signed up the row
-- was missing and /auth/verify-otp fell into the new-user branch and rejected
-- the login with "Name is required for new users".

INSERT INTO auth.users (phone, email, name, role, kyc_status, language, is_active)
VALUES ('+917367034295', 'sawancse@gmail.com', 'Admin', 'ADMIN', 'VERIFIED', 'en', TRUE)
ON CONFLICT (phone) WHERE phone IS NOT NULL
DO UPDATE SET role = 'ADMIN', email = EXCLUDED.email;
