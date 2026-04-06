-- V77: Fix VAS table schemas to match JPA entities
-- Adds missing columns across all VAS tables (V72-V75)

-- ═══════════════════════════════════════════════════════════════
-- agreement_requests (V72) — entity: AgreementRequest.java
-- ═══════════════════════════════════════════════════════════════
ALTER TABLE agreement_requests ADD COLUMN IF NOT EXISTS state VARCHAR(100);
ALTER TABLE agreement_requests ADD COLUMN IF NOT EXISTS city VARCHAR(100);
ALTER TABLE agreement_requests ADD COLUMN IF NOT EXISTS agreement_date DATE;
ALTER TABLE agreement_requests ADD COLUMN IF NOT EXISTS start_date DATE;
ALTER TABLE agreement_requests ADD COLUMN IF NOT EXISTS end_date DATE;
ALTER TABLE agreement_requests ADD COLUMN IF NOT EXISTS monthly_rent_paise BIGINT;
ALTER TABLE agreement_requests ADD COLUMN IF NOT EXISTS security_deposit_paise BIGINT;
ALTER TABLE agreement_requests ADD COLUMN IF NOT EXISTS sale_consideration_paise BIGINT;
ALTER TABLE agreement_requests ADD COLUMN IF NOT EXISTS razorpay_order_id VARCHAR(100);
ALTER TABLE agreement_requests ADD COLUMN IF NOT EXISTS razorpay_payment_id VARCHAR(100);
ALTER TABLE agreement_requests ADD COLUMN IF NOT EXISTS notes TEXT;

-- ═══════════════════════════════════════════════════════════════
-- agreement_parties (V72) — entity: AgreementParty.java
-- ═══════════════════════════════════════════════════════════════
ALTER TABLE agreement_parties ADD COLUMN IF NOT EXISTS father_name VARCHAR(200);
ALTER TABLE agreement_parties ADD COLUMN IF NOT EXISTS id_proof_url TEXT;
ALTER TABLE agreement_parties ADD COLUMN IF NOT EXISTS photo_url TEXT;
ALTER TABLE agreement_parties ADD COLUMN IF NOT EXISTS e_sign_request_id VARCHAR(100);
ALTER TABLE agreement_parties ADD COLUMN IF NOT EXISTS signed_at TIMESTAMPTZ;
ALTER TABLE agreement_parties ADD COLUMN IF NOT EXISTS sort_order INTEGER DEFAULT 0;
ALTER TABLE agreement_parties ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT NOW();

-- ═══════════════════════════════════════════════════════════════
-- stamp_duty_configs (V72) — entity: StampDutyConfig.java
-- ═══════════════════════════════════════════════════════════════
ALTER TABLE stamp_duty_configs ADD COLUMN IF NOT EXISTS minimum_stamp_paise BIGINT;
ALTER TABLE stamp_duty_configs ADD COLUMN IF NOT EXISTS maximum_stamp_paise BIGINT;
ALTER TABLE stamp_duty_configs ADD COLUMN IF NOT EXISTS cess_percent DECIMAL(5,2);
ALTER TABLE stamp_duty_configs ADD COLUMN IF NOT EXISTS notes TEXT;
ALTER TABLE stamp_duty_configs ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT NOW();
ALTER TABLE stamp_duty_configs ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT NOW();

-- ═══════════════════════════════════════════════════════════════
-- partner_banks (V73) — entity: PartnerBank.java
-- ═══════════════════════════════════════════════════════════════
ALTER TABLE partner_banks ADD COLUMN IF NOT EXISTS processing_fee_min_paise BIGINT;
ALTER TABLE partner_banks ADD COLUMN IF NOT EXISTS processing_fee_max_paise BIGINT;
ALTER TABLE partner_banks ADD COLUMN IF NOT EXISTS pre_approval_available BOOLEAN DEFAULT FALSE;
ALTER TABLE partner_banks ADD COLUMN IF NOT EXISTS balance_transfer_available BOOLEAN DEFAULT FALSE;
ALTER TABLE partner_banks ADD COLUMN IF NOT EXISTS contact_name VARCHAR(200);
ALTER TABLE partner_banks ADD COLUMN IF NOT EXISTS contact_email VARCHAR(200);
ALTER TABLE partner_banks ADD COLUMN IF NOT EXISTS contact_phone VARCHAR(20);
ALTER TABLE partner_banks ADD COLUMN IF NOT EXISTS sort_order INTEGER DEFAULT 0;
ALTER TABLE partner_banks ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT NOW();

-- ═══════════════════════════════════════════════════════════════
-- loan_eligibilities (V73) — entity: LoanEligibility.java
-- ═══════════════════════════════════════════════════════════════
ALTER TABLE loan_eligibilities ADD COLUMN IF NOT EXISTS credit_score INTEGER DEFAULT 0;
ALTER TABLE loan_eligibilities ADD COLUMN IF NOT EXISTS max_ltv_percent DECIMAL(5,2);
ALTER TABLE loan_eligibilities ADD COLUMN IF NOT EXISTS offered_interest_rate DECIMAL(5,2);
ALTER TABLE loan_eligibilities ADD COLUMN IF NOT EXISTS offered_tenure_months INTEGER;
ALTER TABLE loan_eligibilities ADD COLUMN IF NOT EXISTS partner_bank_id UUID;
ALTER TABLE loan_eligibilities ADD COLUMN IF NOT EXISTS eligibility_details_json JSONB;
ALTER TABLE loan_eligibilities ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT NOW();
ALTER TABLE loan_eligibilities ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT NOW();

-- ═══════════════════════════════════════════════════════════════
-- loan_applications (V73) — entity: LoanApplication.java
-- ═══════════════════════════════════════════════════════════════
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT NOW();
-- Copy applied_at to created_at for existing rows
UPDATE loan_applications SET created_at = applied_at WHERE created_at IS NULL AND applied_at IS NOT NULL;
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS application_number VARCHAR(50);
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS employment_type VARCHAR(30);
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS sanctioned_interest_rate DECIMAL(5,2);
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS sanctioned_tenure_months INTEGER;
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS estimated_emi_paise BIGINT;
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS property_value_paise BIGINT;
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS pan_number VARCHAR(15);
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS monthly_income_paise BIGINT;
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS employer_name VARCHAR(200);
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS work_experience_years INTEGER;
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS remarks TEXT;
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS bank_reference_id VARCHAR(100);
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS sanctioned_at TIMESTAMPTZ;
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS disbursed_at TIMESTAMPTZ;

-- ═══════════════════════════════════════════════════════════════
-- loan_documents (V73) — entity: LoanDocument.java
-- ═══════════════════════════════════════════════════════════════
ALTER TABLE loan_documents ADD COLUMN IF NOT EXISTS file_name VARCHAR(200);
ALTER TABLE loan_documents ADD COLUMN IF NOT EXISTS file_size_bytes BIGINT;
ALTER TABLE loan_documents ADD COLUMN IF NOT EXISTS mime_type VARCHAR(50);
ALTER TABLE loan_documents ADD COLUMN IF NOT EXISTS verified BOOLEAN DEFAULT FALSE;
ALTER TABLE loan_documents ADD COLUMN IF NOT EXISTS verified_by VARCHAR(200);
ALTER TABLE loan_documents ADD COLUMN IF NOT EXISTS verified_at TIMESTAMPTZ;
ALTER TABLE loan_documents ADD COLUMN IF NOT EXISTS rejection_reason TEXT;
ALTER TABLE loan_documents ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT NOW();
ALTER TABLE loan_documents ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT NOW();

-- ═══════════════════════════════════════════════════════════════
-- advocates (V74) — entity: Advocate.java
-- ═══════════════════════════════════════════════════════════════
ALTER TABLE advocates ADD COLUMN IF NOT EXISTS address VARCHAR(500);
ALTER TABLE advocates ADD COLUMN IF NOT EXISTS experience_years INTEGER DEFAULT 0;
ALTER TABLE advocates ADD COLUMN IF NOT EXISTS profile_photo_url TEXT;
ALTER TABLE advocates ADD COLUMN IF NOT EXISTS total_cases INTEGER DEFAULT 0;
ALTER TABLE advocates ADD COLUMN IF NOT EXISTS verified BOOLEAN DEFAULT FALSE;
ALTER TABLE advocates ADD COLUMN IF NOT EXISTS consultation_fee_paise BIGINT;
ALTER TABLE advocates ADD COLUMN IF NOT EXISTS bio TEXT;
ALTER TABLE advocates ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT NOW();

-- ═══════════════════════════════════════════════════════════════
-- legal_cases (V74) — entity: LegalCase.java
-- ═══════════════════════════════════════════════════════════════
ALTER TABLE legal_cases ADD COLUMN IF NOT EXISTS listing_id UUID;
ALTER TABLE legal_cases ADD COLUMN IF NOT EXISTS report_summary TEXT;
ALTER TABLE legal_cases ADD COLUMN IF NOT EXISTS razorpay_order_id VARCHAR(100);
ALTER TABLE legal_cases ADD COLUMN IF NOT EXISTS razorpay_payment_id VARCHAR(100);
ALTER TABLE legal_cases ADD COLUMN IF NOT EXISTS paid BOOLEAN DEFAULT FALSE;
ALTER TABLE legal_cases ADD COLUMN IF NOT EXISTS assigned_at TIMESTAMPTZ;
ALTER TABLE legal_cases ADD COLUMN IF NOT EXISTS report_ready_at TIMESTAMPTZ;
ALTER TABLE legal_cases ADD COLUMN IF NOT EXISTS closed_at TIMESTAMPTZ;
ALTER TABLE legal_cases ADD COLUMN IF NOT EXISTS notes TEXT;

-- ═══════════════════════════════════════════════════════════════
-- legal_documents (V74) — entity: LegalDocument.java
-- ═══════════════════════════════════════════════════════════════
ALTER TABLE legal_documents ADD COLUMN IF NOT EXISTS file_name VARCHAR(200);
ALTER TABLE legal_documents ADD COLUMN IF NOT EXISTS file_size_bytes BIGINT;
ALTER TABLE legal_documents ADD COLUMN IF NOT EXISTS mime_type VARCHAR(50);
ALTER TABLE legal_documents ADD COLUMN IF NOT EXISTS uploaded_by_user BOOLEAN DEFAULT TRUE;
ALTER TABLE legal_documents ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT NOW();
ALTER TABLE legal_documents ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT NOW();

-- ═══════════════════════════════════════════════════════════════
-- legal_verifications (V74) — entity: LegalVerification.java
-- ═══════════════════════════════════════════════════════════════
ALTER TABLE legal_verifications ADD COLUMN IF NOT EXISTS risk_level VARCHAR(10);
ALTER TABLE legal_verifications ADD COLUMN IF NOT EXISTS findings TEXT;
ALTER TABLE legal_verifications ADD COLUMN IF NOT EXISTS recommendation TEXT;
ALTER TABLE legal_verifications ADD COLUMN IF NOT EXISTS document_url TEXT;
ALTER TABLE legal_verifications ADD COLUMN IF NOT EXISTS verified_by VARCHAR(200);
ALTER TABLE legal_verifications ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT NOW();
ALTER TABLE legal_verifications ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT NOW();

-- ═══════════════════════════════════════════════════════════════
-- legal_consultations (V74) — entity: LegalConsultation.java
-- ═══════════════════════════════════════════════════════════════
ALTER TABLE legal_consultations ADD COLUMN IF NOT EXISTS user_id UUID;
ALTER TABLE legal_consultations ADD COLUMN IF NOT EXISTS mode VARCHAR(20);
ALTER TABLE legal_consultations ADD COLUMN IF NOT EXISTS advice_given TEXT;
ALTER TABLE legal_consultations ADD COLUMN IF NOT EXISTS fee_paise BIGINT;
ALTER TABLE legal_consultations ADD COLUMN IF NOT EXISTS completed_at TIMESTAMPTZ;
ALTER TABLE legal_consultations ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT NOW();

-- ═══════════════════════════════════════════════════════════════
-- interior_designers (V75) — entity: InteriorDesigner.java
-- ═══════════════════════════════════════════════════════════════
ALTER TABLE interior_designers ADD COLUMN IF NOT EXISTS company_name VARCHAR(200);
ALTER TABLE interior_designers ADD COLUMN IF NOT EXISTS address VARCHAR(500);
ALTER TABLE interior_designers ADD COLUMN IF NOT EXISTS state VARCHAR(100);
ALTER TABLE interior_designers ADD COLUMN IF NOT EXISTS profile_photo_url TEXT;
ALTER TABLE interior_designers ADD COLUMN IF NOT EXISTS total_projects INTEGER DEFAULT 0;
ALTER TABLE interior_designers ADD COLUMN IF NOT EXISTS consultation_fee_paise BIGINT;
ALTER TABLE interior_designers ADD COLUMN IF NOT EXISTS verified BOOLEAN DEFAULT FALSE;
ALTER TABLE interior_designers ADD COLUMN IF NOT EXISTS bio TEXT;
ALTER TABLE interior_designers ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT NOW();

-- ═══════════════════════════════════════════════════════════════
-- interior_projects (V75) — entity: InteriorProject.java
-- ═══════════════════════════════════════════════════════════════
ALTER TABLE interior_projects ADD COLUMN IF NOT EXISTS listing_id UUID;
ALTER TABLE interior_projects ADD COLUMN IF NOT EXISTS sale_property_id UUID;
ALTER TABLE interior_projects ADD COLUMN IF NOT EXISTS design_style VARCHAR(30);
ALTER TABLE interior_projects ADD COLUMN IF NOT EXISTS area_sqft INTEGER;
ALTER TABLE interior_projects ADD COLUMN IF NOT EXISTS approved_amount_paise BIGINT;
ALTER TABLE interior_projects ADD COLUMN IF NOT EXISTS requirements TEXT;
ALTER TABLE interior_projects ADD COLUMN IF NOT EXISTS current_photos TEXT[];
ALTER TABLE interior_projects ADD COLUMN IF NOT EXISTS expected_start_date DATE;
ALTER TABLE interior_projects ADD COLUMN IF NOT EXISTS expected_end_date DATE;
ALTER TABLE interior_projects ADD COLUMN IF NOT EXISTS actual_start_date DATE;
ALTER TABLE interior_projects ADD COLUMN IF NOT EXISTS actual_end_date DATE;
ALTER TABLE interior_projects ADD COLUMN IF NOT EXISTS razorpay_order_id VARCHAR(100);
ALTER TABLE interior_projects ADD COLUMN IF NOT EXISTS razorpay_payment_id VARCHAR(100);
ALTER TABLE interior_projects ADD COLUMN IF NOT EXISTS paid BOOLEAN DEFAULT FALSE;
ALTER TABLE interior_projects ADD COLUMN IF NOT EXISTS notes TEXT;

-- ═══════════════════════════════════════════════════════════════
-- room_designs (V75) — entity: RoomDesign.java
-- ═══════════════════════════════════════════════════════════════
ALTER TABLE room_designs ADD COLUMN IF NOT EXISTS room_name VARCHAR(200);
ALTER TABLE room_designs ADD COLUMN IF NOT EXISTS design_2d_url TEXT;
ALTER TABLE room_designs ADD COLUMN IF NOT EXISTS design_3d_url TEXT;
ALTER TABLE room_designs ADD COLUMN IF NOT EXISTS photos TEXT[];
ALTER TABLE room_designs ADD COLUMN IF NOT EXISTS specifications TEXT;
ALTER TABLE room_designs ADD COLUMN IF NOT EXISTS estimated_cost_paise BIGINT;
ALTER TABLE room_designs ADD COLUMN IF NOT EXISTS approved BOOLEAN DEFAULT FALSE;
ALTER TABLE room_designs ADD COLUMN IF NOT EXISTS approved_at TIMESTAMPTZ;
ALTER TABLE room_designs ADD COLUMN IF NOT EXISTS feedback TEXT;
ALTER TABLE room_designs ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT NOW();

-- ═══════════════════════════════════════════════════════════════
-- materials_catalog (V75) — entity: MaterialCatalog.java
-- ═══════════════════════════════════════════════════════════════
ALTER TABLE materials_catalog ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE materials_catalog ADD COLUMN IF NOT EXISTS sku VARCHAR(100);
ALTER TABLE materials_catalog ADD COLUMN IF NOT EXISTS photos TEXT[];
ALTER TABLE materials_catalog ADD COLUMN IF NOT EXISTS color VARCHAR(200);
ALTER TABLE materials_catalog ADD COLUMN IF NOT EXISTS dimensions VARCHAR(200);
ALTER TABLE materials_catalog ADD COLUMN IF NOT EXISTS warranty VARCHAR(100);
ALTER TABLE materials_catalog ADD COLUMN IF NOT EXISTS sort_order INTEGER DEFAULT 0;
ALTER TABLE materials_catalog ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT NOW();
ALTER TABLE materials_catalog ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT NOW();

-- ═══════════════════════════════════════════════════════════════
-- material_selections (V75) — entity: MaterialSelection.java
-- ═══════════════════════════════════════════════════════════════
ALTER TABLE material_selections ADD COLUMN IF NOT EXISTS notes TEXT;
ALTER TABLE material_selections ADD COLUMN IF NOT EXISTS approved BOOLEAN DEFAULT FALSE;
ALTER TABLE material_selections ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT NOW();

-- ═══════════════════════════════════════════════════════════════
-- interior_quotes (V75) — entity: InteriorQuote.java
-- ═══════════════════════════════════════════════════════════════
ALTER TABLE interior_quotes ADD COLUMN IF NOT EXISTS quote_number VARCHAR(50);
ALTER TABLE interior_quotes ADD COLUMN IF NOT EXISTS tax_paise BIGINT DEFAULT 0;
ALTER TABLE interior_quotes ADD COLUMN IF NOT EXISTS line_items_json JSONB;
ALTER TABLE interior_quotes ADD COLUMN IF NOT EXISTS terms_json JSONB;
ALTER TABLE interior_quotes ADD COLUMN IF NOT EXISTS notes TEXT;
ALTER TABLE interior_quotes ADD COLUMN IF NOT EXISTS sent_at TIMESTAMPTZ;
ALTER TABLE interior_quotes ADD COLUMN IF NOT EXISTS approved_at TIMESTAMPTZ;
ALTER TABLE interior_quotes ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT NOW();

-- ═══════════════════════════════════════════════════════════════
-- project_milestones (V75) — entity: ProjectMilestone.java
-- ═══════════════════════════════════════════════════════════════
ALTER TABLE project_milestones ADD COLUMN IF NOT EXISTS sort_order INTEGER DEFAULT 0;
ALTER TABLE project_milestones ADD COLUMN IF NOT EXISTS completion_percent DECIMAL(5,2);
ALTER TABLE project_milestones ADD COLUMN IF NOT EXISTS payment_done BOOLEAN DEFAULT FALSE;
ALTER TABLE project_milestones ADD COLUMN IF NOT EXISTS notes TEXT;
ALTER TABLE project_milestones ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT NOW();

-- ═══════════════════════════════════════════════════════════════
-- quality_checks (V75) — entity: QualityCheck.java
-- ═══════════════════════════════════════════════════════════════
ALTER TABLE quality_checks ADD COLUMN IF NOT EXISTS inspector_name VARCHAR(200);
ALTER TABLE quality_checks ADD COLUMN IF NOT EXISTS findings TEXT;
ALTER TABLE quality_checks ADD COLUMN IF NOT EXISTS rework_notes TEXT;
ALTER TABLE quality_checks ADD COLUMN IF NOT EXISTS rework_completed_at TIMESTAMPTZ;
ALTER TABLE quality_checks ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT NOW();
ALTER TABLE quality_checks ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT NOW();
