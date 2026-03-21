-- Email chapter tracking per booking
CREATE TABLE notifications.email_chapters (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL,
    guest_id UUID NOT NULL,
    host_id UUID NOT NULL,
    listing_id UUID NOT NULL,
    chapter_number INTEGER NOT NULL,
    chapter_name VARCHAR(100) NOT NULL,
    sent_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    email_to VARCHAR(255) NOT NULL,
    tone VARCHAR(20) NOT NULL DEFAULT 'FORMAL',
    UNIQUE(booking_id, chapter_number)
);
CREATE INDEX idx_email_chapters_booking ON notifications.email_chapters(booking_id);
CREATE INDEX idx_email_chapters_guest ON notifications.email_chapters(guest_id);

-- Guest milestones tracking
CREATE TABLE notifications.guest_milestones (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    guest_id UUID NOT NULL,
    milestone_type VARCHAR(50) NOT NULL,
    milestone_value INTEGER NOT NULL,
    achieved_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    notified BOOLEAN NOT NULL DEFAULT false,
    UNIQUE(guest_id, milestone_type, milestone_value)
);
CREATE INDEX idx_guest_milestones_guest ON notifications.guest_milestones(guest_id);

-- Host milestones tracking
CREATE TABLE notifications.host_milestones (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    host_id UUID NOT NULL,
    milestone_type VARCHAR(50) NOT NULL,
    milestone_value INTEGER NOT NULL,
    achieved_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    notified BOOLEAN NOT NULL DEFAULT false,
    UNIQUE(host_id, milestone_type, milestone_value)
);
CREATE INDEX idx_host_milestones_host ON notifications.host_milestones(host_id);

-- Festival calendar
CREATE TABLE notifications.festival_calendar (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    festival_name VARCHAR(100) NOT NULL,
    festival_date DATE NOT NULL,
    region VARCHAR(50),
    language_code VARCHAR(10),
    campaign_subject VARCHAR(255) NOT NULL,
    campaign_headline VARCHAR(255) NOT NULL,
    campaign_body TEXT NOT NULL,
    discovery_categories VARCHAR(500),
    target_cities VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_festival_date ON notifications.festival_calendar(festival_date);
CREATE INDEX idx_festival_region ON notifications.festival_calendar(region);

-- Email preferences per user
CREATE TABLE notifications.email_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE,
    tone_preference VARCHAR(20) DEFAULT 'AUTO',
    marketing_emails BOOLEAN NOT NULL DEFAULT true,
    milestone_emails BOOLEAN NOT NULL DEFAULT true,
    mid_stay_checks BOOLEAN NOT NULL DEFAULT true,
    re_engagement_emails BOOLEAN NOT NULL DEFAULT true,
    festival_emails BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_email_prefs_user ON notifications.email_preferences(user_id);

-- Scheduled email queue
CREATE TABLE notifications.scheduled_emails (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID,
    user_id UUID NOT NULL,
    email_type VARCHAR(50) NOT NULL,
    scheduled_for TIMESTAMPTZ NOT NULL,
    sent BOOLEAN NOT NULL DEFAULT false,
    sent_at TIMESTAMPTZ,
    cancelled BOOLEAN NOT NULL DEFAULT false,
    context_json TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_scheduled_pending ON notifications.scheduled_emails(scheduled_for) WHERE sent = false AND cancelled = false;
CREATE INDEX idx_scheduled_booking ON notifications.scheduled_emails(booking_id);
