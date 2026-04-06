-- Event bookings can be created as INQUIRY without a chef assigned
ALTER TABLE chefs.event_bookings ALTER COLUMN chef_id DROP NOT NULL;
