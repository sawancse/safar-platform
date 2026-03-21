ALTER TABLE users.profiles ADD COLUMN display_name VARCHAR(100);
ALTER TABLE users.profiles ADD COLUMN date_of_birth DATE;
ALTER TABLE users.profiles ADD COLUMN gender VARCHAR(30);
ALTER TABLE users.profiles ADD COLUMN nationality VARCHAR(60) DEFAULT 'India';
ALTER TABLE users.profiles ADD COLUMN address TEXT;
ALTER TABLE users.profiles ADD COLUMN passport_name VARCHAR(200);
ALTER TABLE users.profiles ADD COLUMN passport_number VARCHAR(30);
ALTER TABLE users.profiles ADD COLUMN passport_expiry DATE;
