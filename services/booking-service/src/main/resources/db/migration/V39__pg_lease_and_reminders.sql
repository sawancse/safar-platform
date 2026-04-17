-- Phase 1: PG lease duration tracking on bookings
ALTER TABLE bookings.bookings ADD COLUMN lease_duration_months INTEGER;

-- Phase 3: Payment reminder idempotency flags on invoices
ALTER TABLE bookings.tenancy_invoices ADD COLUMN reminder_5d_sent BOOLEAN DEFAULT false;
ALTER TABLE bookings.tenancy_invoices ADD COLUMN reminder_1d_sent BOOLEAN DEFAULT false;
