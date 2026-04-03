-- Maintenance/complaint requests from PG tenants
CREATE TABLE IF NOT EXISTS bookings.maintenance_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenancy_id UUID NOT NULL,
    request_number VARCHAR(20) UNIQUE,
    category VARCHAR(30) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    photo_urls TEXT,
    priority VARCHAR(10) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    assigned_to VARCHAR(200),
    assigned_at TIMESTAMPTZ,
    resolved_at TIMESTAMPTZ,
    resolution_notes TEXT,
    tenant_rating INT,
    tenant_feedback VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_mr_tenancy FOREIGN KEY (tenancy_id) REFERENCES bookings.pg_tenancies(id)
);

CREATE INDEX idx_mr_tenancy ON bookings.maintenance_requests(tenancy_id);
CREATE INDEX idx_mr_status ON bookings.maintenance_requests(status);
CREATE SEQUENCE IF NOT EXISTS bookings.maintenance_req_seq START WITH 1000 INCREMENT BY 1;
