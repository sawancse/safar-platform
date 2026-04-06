CREATE TABLE users.broker_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE,
    company_name VARCHAR(200),
    rera_agent_id VARCHAR(50),
    rera_verified BOOLEAN DEFAULT FALSE,
    operating_cities TEXT[],
    specialization VARCHAR(20) DEFAULT 'RESIDENTIAL',
    experience_years INT DEFAULT 0,
    total_deals_closed INT DEFAULT 0,
    bio TEXT,
    website VARCHAR(300),
    office_address TEXT,
    office_city VARCHAR(100),
    office_state VARCHAR(100),
    office_pincode VARCHAR(10),
    subscription_tier VARCHAR(20) DEFAULT 'FREE',
    verified BOOLEAN DEFAULT FALSE,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_broker_profiles_user ON users.broker_profiles(user_id);
CREATE INDEX idx_broker_profiles_city ON users.broker_profiles USING GIN(operating_cities);
