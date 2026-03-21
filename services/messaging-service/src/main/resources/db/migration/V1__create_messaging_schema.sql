CREATE SCHEMA IF NOT EXISTS messages;

CREATE TABLE messages.conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    participant1_id UUID NOT NULL,
    participant2_id UUID NOT NULL,
    listing_id UUID NOT NULL,
    booking_id UUID,
    last_message_text TEXT,
    last_message_at TIMESTAMPTZ,
    participant1_unread INTEGER DEFAULT 0,
    participant2_unread INTEGER DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now(),
    UNIQUE(participant1_id, participant2_id, listing_id)
);
CREATE INDEX idx_conv_p1 ON messages.conversations(participant1_id);
CREATE INDEX idx_conv_p2 ON messages.conversations(participant2_id);

CREATE TABLE messages.messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL REFERENCES messages.conversations(id),
    sender_id UUID NOT NULL,
    content TEXT NOT NULL,
    message_type VARCHAR(20) DEFAULT 'TEXT',
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX idx_msg_conv ON messages.messages(conversation_id, created_at);

CREATE TABLE messages.quick_reply_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    content TEXT NOT NULL,
    sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX idx_qrt_user ON messages.quick_reply_templates(user_id);
