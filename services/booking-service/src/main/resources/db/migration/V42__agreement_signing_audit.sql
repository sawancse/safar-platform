-- Track WHO signed (userId) for audit trail + optimistic locking
ALTER TABLE bookings.tenancy_agreements ADD COLUMN host_signed_by UUID;
ALTER TABLE bookings.tenancy_agreements ADD COLUMN tenant_signed_by UUID;
ALTER TABLE bookings.tenancy_agreements ADD COLUMN version BIGINT DEFAULT 0;
