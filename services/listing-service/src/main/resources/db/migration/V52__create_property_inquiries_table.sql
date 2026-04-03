-- V52: Property Inquiries for buy/sell marketplace
CREATE TABLE property_inquiries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sale_property_id UUID NOT NULL REFERENCES sale_properties(id),
    buyer_id UUID NOT NULL,
    seller_id UUID NOT NULL,
    status VARCHAR(20) DEFAULT 'NEW',
    message TEXT,
    buyer_name VARCHAR(100),
    buyer_phone VARCHAR(15),
    buyer_email VARCHAR(200),
    preferred_visit_date DATE,
    preferred_visit_time VARCHAR(20),
    financing_type VARCHAR(15),
    budget_min_paise BIGINT,
    budget_max_paise BIGINT,
    notes TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_inquiries_property ON property_inquiries(sale_property_id);
CREATE INDEX idx_inquiries_buyer ON property_inquiries(buyer_id);
CREATE INDEX idx_inquiries_seller ON property_inquiries(seller_id);
CREATE INDEX idx_inquiries_status ON property_inquiries(status);
