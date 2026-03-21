-- Increase room type name length to support promotional/descriptive names
ALTER TABLE listings.room_types ALTER COLUMN name TYPE VARCHAR(255);
