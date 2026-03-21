-- Profile enrichment: bio, languages, response metrics, profile completion
ALTER TABLE users.profiles ADD COLUMN IF NOT EXISTS bio TEXT;
ALTER TABLE users.profiles ADD COLUMN IF NOT EXISTS languages VARCHAR(200);
ALTER TABLE users.profiles ADD COLUMN IF NOT EXISTS response_rate INTEGER DEFAULT 0;
ALTER TABLE users.profiles ADD COLUMN IF NOT EXISTS avg_response_minutes INTEGER;
ALTER TABLE users.profiles ADD COLUMN IF NOT EXISTS total_host_reviews INTEGER DEFAULT 0;
ALTER TABLE users.profiles ADD COLUMN IF NOT EXISTS last_active_at TIMESTAMPTZ;
ALTER TABLE users.profiles ADD COLUMN IF NOT EXISTS profile_completion INTEGER DEFAULT 0;
