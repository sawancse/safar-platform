-- Add modification tracking to all booking tables
ALTER TABLE chefs.chef_bookings ADD COLUMN modified_at TIMESTAMPTZ;
ALTER TABLE chefs.chef_bookings ADD COLUMN modification_count INTEGER DEFAULT 0;

ALTER TABLE chefs.event_bookings ADD COLUMN modified_at TIMESTAMPTZ;
ALTER TABLE chefs.event_bookings ADD COLUMN modification_count INTEGER DEFAULT 0;

ALTER TABLE chefs.chef_subscriptions ADD COLUMN modified_at TIMESTAMPTZ;
ALTER TABLE chefs.chef_subscriptions ADD COLUMN modification_count INTEGER DEFAULT 0;
