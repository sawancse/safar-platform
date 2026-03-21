CREATE TABLE IF NOT EXISTS medical_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE,
    blood_group VARCHAR(10),
    allergies TEXT,
    current_medications TEXT,
    past_surgeries TEXT,
    chronic_conditions TEXT,
    emergency_contact_name VARCHAR(255),
    emergency_contact_phone VARCHAR(20),
    emergency_contact_relation VARCHAR(50),
    preferred_language VARCHAR(50) DEFAULT 'English',
    dietary_restrictions TEXT,
    mobility_needs TEXT,
    insurance_provider VARCHAR(255),
    insurance_policy_number VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_medical_profiles_user_id ON medical_profiles(user_id);
