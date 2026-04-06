-- =============================================
-- V34: Upgrade maintenance_requests to full ticket system (Zolo PG style)
-- =============================================

-- 1. Add SLA and escalation columns to maintenance_requests
ALTER TABLE bookings.maintenance_requests ADD COLUMN listing_id UUID;
ALTER TABLE bookings.maintenance_requests ADD COLUMN sla_deadline_at TIMESTAMPTZ;
ALTER TABLE bookings.maintenance_requests ADD COLUMN sla_breached BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE bookings.maintenance_requests ADD COLUMN escalation_level INT NOT NULL DEFAULT 1;
ALTER TABLE bookings.maintenance_requests ADD COLUMN escalated_at TIMESTAMPTZ;
ALTER TABLE bookings.maintenance_requests ADD COLUMN reopened_at TIMESTAMPTZ;
ALTER TABLE bookings.maintenance_requests ADD COLUMN reopen_count INT NOT NULL DEFAULT 0;
ALTER TABLE bookings.maintenance_requests ADD COLUMN closed_at TIMESTAMPTZ;

-- 2. Migrate ACKNOWLEDGED → ASSIGNED
UPDATE bookings.maintenance_requests SET status = 'ASSIGNED' WHERE status = 'ACKNOWLEDGED';

-- 3. Ticket comments / activity log
CREATE TABLE IF NOT EXISTS bookings.ticket_comments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id UUID NOT NULL,
    author_id UUID NOT NULL,
    author_role VARCHAR(20) NOT NULL,
    comment_text TEXT NOT NULL,
    attachment_urls TEXT,
    is_system_note BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_tc_request FOREIGN KEY (request_id)
        REFERENCES bookings.maintenance_requests(id) ON DELETE CASCADE
);

CREATE INDEX idx_tc_request ON bookings.ticket_comments(request_id);
CREATE INDEX idx_mr_listing ON bookings.maintenance_requests(listing_id);
CREATE INDEX idx_mr_sla_breach ON bookings.maintenance_requests(sla_breached) WHERE sla_breached = TRUE;
CREATE INDEX idx_mr_escalation ON bookings.maintenance_requests(escalation_level);
