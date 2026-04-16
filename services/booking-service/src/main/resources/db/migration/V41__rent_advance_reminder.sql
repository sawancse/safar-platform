-- Track whether 7-day advance rent reminder was sent for the current billing cycle
ALTER TABLE bookings.pg_tenancies ADD COLUMN rent_advance_reminder_sent BOOLEAN DEFAULT false;
