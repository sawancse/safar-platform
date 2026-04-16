-- Add tenant_id to tenancy_invoices so Kafka events carry tenant identity for notifications
ALTER TABLE bookings.tenancy_invoices ADD COLUMN tenant_id UUID;

-- Backfill from pg_tenancies
UPDATE bookings.tenancy_invoices ti
SET tenant_id = pt.tenant_id
FROM bookings.pg_tenancies pt
WHERE ti.tenancy_id = pt.id;

-- Make non-null going forward
ALTER TABLE bookings.tenancy_invoices ALTER COLUMN tenant_id SET NOT NULL;
