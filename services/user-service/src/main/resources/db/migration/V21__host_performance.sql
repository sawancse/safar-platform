ALTER TABLE users.host_subscriptions ADD COLUMN commission_discount_percent INTEGER DEFAULT 0;
ALTER TABLE users.host_subscriptions ADD COLUMN preferred_partner BOOLEAN DEFAULT FALSE;
ALTER TABLE users.host_subscriptions ADD COLUMN avg_host_rating DECIMAL(3,1);
ALTER TABLE users.host_subscriptions ADD COLUMN performance_updated_at TIMESTAMPTZ;
