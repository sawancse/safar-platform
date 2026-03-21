-- Channel Manager Phase 2: Channex.io integration
CREATE TABLE IF NOT EXISTS listings.channel_manager_properties (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id UUID NOT NULL,
    channex_property_id VARCHAR(100),
    channex_group_id VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    connected_channels TEXT, -- JSON array
    last_sync_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_cmp_listing FOREIGN KEY (listing_id) REFERENCES listings.listings(id)
);

CREATE TABLE IF NOT EXISTS listings.channel_mappings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    channel_manager_property_id UUID NOT NULL,
    channel_name VARCHAR(30) NOT NULL,
    channel_property_id VARCHAR(100),
    channel_room_type_id VARCHAR(100),
    local_room_type_id UUID,
    rate_sync BOOLEAN NOT NULL DEFAULT TRUE,
    availability_sync BOOLEAN NOT NULL DEFAULT TRUE,
    content_sync BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_cm_property FOREIGN KEY (channel_manager_property_id) REFERENCES listings.channel_manager_properties(id)
);

CREATE TABLE IF NOT EXISTS listings.channel_sync_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    channel_manager_property_id UUID NOT NULL,
    direction VARCHAR(10) NOT NULL,
    sync_type VARCHAR(20) NOT NULL,
    channel_name VARCHAR(30),
    success BOOLEAN NOT NULL DEFAULT TRUE,
    error_message TEXT,
    records_affected INT NOT NULL DEFAULT 0,
    synced_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_csl_property FOREIGN KEY (channel_manager_property_id) REFERENCES listings.channel_manager_properties(id)
);

CREATE INDEX idx_cmp_listing ON listings.channel_manager_properties(listing_id);
CREATE INDEX idx_cmp_status ON listings.channel_manager_properties(status);
CREATE INDEX idx_cm_channel ON listings.channel_mappings(channel_name);
CREATE INDEX idx_csl_synced ON listings.channel_sync_logs(synced_at);
