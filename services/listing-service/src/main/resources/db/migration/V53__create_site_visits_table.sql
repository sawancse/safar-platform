-- V53: Site Visits for property buy/sell
CREATE TABLE site_visits (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    inquiry_id UUID REFERENCES property_inquiries(id),
    sale_property_id UUID NOT NULL REFERENCES sale_properties(id),
    buyer_id UUID NOT NULL,
    seller_id UUID NOT NULL,
    scheduled_at TIMESTAMPTZ NOT NULL,
    duration_minutes INTEGER DEFAULT 30,
    status VARCHAR(20) DEFAULT 'REQUESTED',
    buyer_feedback TEXT,
    seller_feedback TEXT,
    rating INTEGER,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_visits_property ON site_visits(sale_property_id);
CREATE INDEX idx_visits_buyer ON site_visits(buyer_id);
CREATE INDEX idx_visits_seller ON site_visits(seller_id);
CREATE INDEX idx_visits_scheduled ON site_visits(scheduled_at);
CREATE INDEX idx_visits_status ON site_visits(status);
