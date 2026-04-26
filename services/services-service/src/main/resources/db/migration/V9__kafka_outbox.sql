-- Kafka outbox: persist events when Kafka is down, retry later
CREATE TABLE IF NOT EXISTS chefs.kafka_outbox (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    topic VARCHAR(200) NOT NULL,
    event_key VARCHAR(200),
    payload TEXT NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    retry_count INTEGER DEFAULT 0,
    max_retries INTEGER DEFAULT 10,
    error_message TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    sent_at TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_chef_outbox_status ON chefs.kafka_outbox(status) WHERE status = 'PENDING';
