ALTER TABLE users.profiles ADD COLUMN IF NOT EXISTS preferred_language VARCHAR(5) DEFAULT 'en';
