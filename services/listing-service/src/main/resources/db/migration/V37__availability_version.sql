-- Add version columns for optimistic locking
ALTER TABLE listings.availability ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
ALTER TABLE listings.room_type_availability ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
