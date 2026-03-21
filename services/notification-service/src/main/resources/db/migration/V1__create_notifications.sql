CREATE SCHEMA IF NOT EXISTS notifications;

CREATE TABLE notifications.notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    body TEXT,
    type VARCHAR(50) NOT NULL,
    reference_id VARCHAR(255),
    reference_type VARCHAR(50),
    read BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_notifications_user_id ON notifications.notifications(user_id);
CREATE INDEX idx_notifications_user_read ON notifications.notifications(user_id, read);
CREATE INDEX idx_notifications_created ON notifications.notifications(created_at DESC);
