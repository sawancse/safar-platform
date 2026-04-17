-- Expand message_type to support FILE, IMAGE, LOCATION
ALTER TABLE messages.messages ALTER COLUMN message_type TYPE VARCHAR(30);

-- Attachment fields for FILE/IMAGE messages
ALTER TABLE messages.messages ADD COLUMN attachment_url TEXT;
ALTER TABLE messages.messages ADD COLUMN attachment_name VARCHAR(255);
ALTER TABLE messages.messages ADD COLUMN attachment_size BIGINT;
ALTER TABLE messages.messages ADD COLUMN attachment_type VARCHAR(50);

-- Location fields for LOCATION messages (chef tracking, property directions)
ALTER TABLE messages.messages ADD COLUMN latitude DOUBLE PRECISION;
ALTER TABLE messages.messages ADD COLUMN longitude DOUBLE PRECISION;
ALTER TABLE messages.messages ADD COLUMN location_label VARCHAR(200);
