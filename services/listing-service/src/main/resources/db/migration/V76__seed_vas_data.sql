-- Seed VAS (Value Added Services) data: partner banks, stamp duty, advocates, designers, materials

-- =====================
-- 15 Partner Banks
-- =====================
INSERT INTO partner_banks (bank_name, logo_url, min_interest_rate, max_interest_rate, processing_fee_percent, max_tenure_months, max_ltv_percent, min_loan_amount_paise, max_loan_amount_paise, min_income_paise, special_offers, commission_percent, features_json, active) VALUES
('State Bank of India', '/images/banks/sbi.png', 8.25, 9.15, 0.35, 360, 90, 500000000, 1000000000000, 2500000, 'No processing fee for women borrowers', 0.80, '{"balance_transfer": true, "top_up": true, "emi_holiday": true, "pre_approved": true}', TRUE),
('HDFC Bank', '/images/banks/hdfc.png', 8.35, 9.25, 0.50, 360, 85, 500000000, 1000000000000, 2500000, 'Instant approval for salaried professionals', 1.00, '{"balance_transfer": true, "top_up": true, "emi_holiday": true, "doorstep_service": true}', TRUE),
('ICICI Bank', '/images/banks/icici.png', 8.40, 9.30, 0.50, 360, 85, 500000000, 500000000000, 2500000, 'Pre-approved offers for existing customers', 1.00, '{"balance_transfer": true, "top_up": true, "online_tracking": true, "instant_disbursal": true}', TRUE),
('Axis Bank', '/images/banks/axis.png', 8.50, 9.35, 0.50, 360, 80, 500000000, 500000000000, 3000000, 'Shubh Aarambh - lower rates for smaller loans', 1.00, '{"balance_transfer": true, "top_up": true, "power_advantage": true}', TRUE),
('Kotak Mahindra Bank', '/images/banks/kotak.png', 8.45, 9.25, 0.50, 360, 80, 500000000, 500000000000, 3000000, 'Flexible EMI options', 1.00, '{"balance_transfer": true, "top_up": true, "flexi_emi": true}', TRUE),
('Punjab National Bank', '/images/banks/pnb.png', 8.30, 9.20, 0.35, 360, 85, 300000000, 500000000000, 2000000, 'Reduced rates for government employees', 0.75, '{"balance_transfer": true, "pradhan_mantri_awas": true}', TRUE),
('Bank of Baroda', '/images/banks/bob.png', 8.30, 9.15, 0.25, 360, 85, 300000000, 500000000000, 2000000, 'Special rates for defence personnel', 0.75, '{"balance_transfer": true, "top_up": true, "baroda_home_loan_advantage": true}', TRUE),
('Canara Bank', '/images/banks/canara.png', 8.35, 9.25, 0.50, 360, 85, 300000000, 500000000000, 2000000, 'Lower rates for women borrowers', 0.70, '{"balance_transfer": true, "pradhan_mantri_awas": true}', TRUE),
('Union Bank of India', '/images/banks/union.png', 8.30, 9.20, 0.50, 360, 85, 200000000, 500000000000, 2000000, 'Zero pre-payment charges', 0.70, '{"balance_transfer": true, "top_up": true}', TRUE),
('Indian Bank', '/images/banks/indian.png', 8.30, 9.15, 0.25, 360, 85, 200000000, 300000000000, 2000000, 'Concession for existing customers', 0.65, '{"balance_transfer": true, "pradhan_mantri_awas": true}', TRUE),
('Indian Overseas Bank', '/images/banks/iob.png', 8.35, 9.25, 0.50, 360, 80, 200000000, 300000000000, 2000000, 'Special scheme for NRIs', 0.65, '{"balance_transfer": true, "nri_loans": true}', TRUE),
('Federal Bank', '/images/banks/federal.png', 8.45, 9.35, 0.50, 360, 80, 500000000, 500000000000, 2500000, 'Quick sanction within 48 hours', 0.90, '{"balance_transfer": true, "top_up": true, "quick_sanction": true}', TRUE),
('IDBI Bank', '/images/banks/idbi.png', 8.35, 9.20, 0.50, 360, 85, 300000000, 500000000000, 2000000, 'Festive season discounts', 0.75, '{"balance_transfer": true, "top_up": true}', TRUE),
('LIC Housing Finance', '/images/banks/lichf.png', 8.35, 9.30, 0.50, 360, 85, 500000000, 500000000000, 2500000, 'LIC policy holders get 0.05% concession', 1.00, '{"balance_transfer": true, "top_up": true, "griha_varishth": true, "griha_lakshmi": true}', TRUE),
('Bajaj Housing Finance', '/images/banks/bajaj.png', 8.50, 9.50, 1.00, 360, 75, 500000000, 500000000000, 3500000, 'Doorstep documentation service', 1.20, '{"balance_transfer": true, "top_up": true, "flexi_loan": true, "doorstep_service": true}', TRUE);

-- =====================
-- Stamp Duty Configs (10 major states, 5 agreement types each)
-- =====================

-- Maharashtra
INSERT INTO stamp_duty_configs (state, agreement_type, duty_percent, registration_percent, surcharge_percent, effective_from, active) VALUES
('Maharashtra', 'SALE_DEED', 5.00, 1.00, 0.00, '2024-01-01', TRUE),
('Maharashtra', 'SALE_AGREEMENT', 0.10, 0.00, 0.00, '2024-01-01', TRUE),
('Maharashtra', 'RENTAL_AGREEMENT', 0.25, 0.00, 0.00, '2024-01-01', TRUE),
('Maharashtra', 'LEAVE_LICENSE', 0.25, 0.00, 0.00, '2024-01-01', TRUE),
('Maharashtra', 'PG_AGREEMENT', 0.10, 0.00, 0.00, '2024-01-01', TRUE);

-- Karnataka
INSERT INTO stamp_duty_configs (state, agreement_type, duty_percent, registration_percent, surcharge_percent, effective_from, active) VALUES
('Karnataka', 'SALE_DEED', 5.00, 1.00, 2.00, '2024-01-01', TRUE),
('Karnataka', 'SALE_AGREEMENT', 0.10, 0.00, 0.00, '2024-01-01', TRUE),
('Karnataka', 'RENTAL_AGREEMENT', 1.00, 0.50, 0.00, '2024-01-01', TRUE),
('Karnataka', 'LEAVE_LICENSE', 0.50, 0.00, 0.00, '2024-01-01', TRUE),
('Karnataka', 'PG_AGREEMENT', 0.10, 0.00, 0.00, '2024-01-01', TRUE);

-- Telangana
INSERT INTO stamp_duty_configs (state, agreement_type, duty_percent, registration_percent, surcharge_percent, effective_from, active) VALUES
('Telangana', 'SALE_DEED', 4.00, 0.50, 0.00, '2024-01-01', TRUE),
('Telangana', 'SALE_AGREEMENT', 0.10, 0.00, 0.00, '2024-01-01', TRUE),
('Telangana', 'RENTAL_AGREEMENT', 0.40, 0.10, 0.00, '2024-01-01', TRUE),
('Telangana', 'LEAVE_LICENSE', 0.40, 0.00, 0.00, '2024-01-01', TRUE),
('Telangana', 'PG_AGREEMENT', 0.10, 0.00, 0.00, '2024-01-01', TRUE);

-- Tamil Nadu
INSERT INTO stamp_duty_configs (state, agreement_type, duty_percent, registration_percent, surcharge_percent, effective_from, active) VALUES
('Tamil Nadu', 'SALE_DEED', 7.00, 4.00, 0.00, '2024-01-01', TRUE),
('Tamil Nadu', 'SALE_AGREEMENT', 1.00, 0.00, 0.00, '2024-01-01', TRUE),
('Tamil Nadu', 'RENTAL_AGREEMENT', 1.00, 1.00, 0.00, '2024-01-01', TRUE),
('Tamil Nadu', 'LEAVE_LICENSE', 1.00, 0.00, 0.00, '2024-01-01', TRUE),
('Tamil Nadu', 'PG_AGREEMENT', 0.10, 0.00, 0.00, '2024-01-01', TRUE);

-- Delhi
INSERT INTO stamp_duty_configs (state, agreement_type, duty_percent, registration_percent, surcharge_percent, effective_from, active) VALUES
('Delhi', 'SALE_DEED', 6.00, 1.00, 0.00, '2024-01-01', TRUE),
('Delhi', 'SALE_AGREEMENT', 0.50, 0.00, 0.00, '2024-01-01', TRUE),
('Delhi', 'RENTAL_AGREEMENT', 2.00, 0.00, 0.00, '2024-01-01', TRUE),
('Delhi', 'LEAVE_LICENSE', 2.00, 0.00, 0.00, '2024-01-01', TRUE),
('Delhi', 'PG_AGREEMENT', 0.10, 0.00, 0.00, '2024-01-01', TRUE);

-- Rajasthan
INSERT INTO stamp_duty_configs (state, agreement_type, duty_percent, registration_percent, surcharge_percent, effective_from, active) VALUES
('Rajasthan', 'SALE_DEED', 5.00, 1.00, 0.00, '2024-01-01', TRUE),
('Rajasthan', 'SALE_AGREEMENT', 0.25, 0.00, 0.00, '2024-01-01', TRUE),
('Rajasthan', 'RENTAL_AGREEMENT', 1.00, 0.00, 0.00, '2024-01-01', TRUE),
('Rajasthan', 'LEAVE_LICENSE', 0.50, 0.00, 0.00, '2024-01-01', TRUE),
('Rajasthan', 'PG_AGREEMENT', 0.10, 0.00, 0.00, '2024-01-01', TRUE);

-- Gujarat
INSERT INTO stamp_duty_configs (state, agreement_type, duty_percent, registration_percent, surcharge_percent, effective_from, active) VALUES
('Gujarat', 'SALE_DEED', 4.90, 1.00, 0.00, '2024-01-01', TRUE),
('Gujarat', 'SALE_AGREEMENT', 0.10, 0.00, 0.00, '2024-01-01', TRUE),
('Gujarat', 'RENTAL_AGREEMENT', 1.00, 0.00, 0.00, '2024-01-01', TRUE),
('Gujarat', 'LEAVE_LICENSE', 0.50, 0.00, 0.00, '2024-01-01', TRUE),
('Gujarat', 'PG_AGREEMENT', 0.10, 0.00, 0.00, '2024-01-01', TRUE);

-- Uttar Pradesh
INSERT INTO stamp_duty_configs (state, agreement_type, duty_percent, registration_percent, surcharge_percent, effective_from, active) VALUES
('Uttar Pradesh', 'SALE_DEED', 7.00, 1.00, 0.00, '2024-01-01', TRUE),
('Uttar Pradesh', 'SALE_AGREEMENT', 0.10, 0.00, 0.00, '2024-01-01', TRUE),
('Uttar Pradesh', 'RENTAL_AGREEMENT', 2.00, 0.00, 0.00, '2024-01-01', TRUE),
('Uttar Pradesh', 'LEAVE_LICENSE', 1.00, 0.00, 0.00, '2024-01-01', TRUE),
('Uttar Pradesh', 'PG_AGREEMENT', 0.10, 0.00, 0.00, '2024-01-01', TRUE);

-- Kerala
INSERT INTO stamp_duty_configs (state, agreement_type, duty_percent, registration_percent, surcharge_percent, effective_from, active) VALUES
('Kerala', 'SALE_DEED', 8.00, 2.00, 0.00, '2024-01-01', TRUE),
('Kerala', 'SALE_AGREEMENT', 0.10, 0.00, 0.00, '2024-01-01', TRUE),
('Kerala', 'RENTAL_AGREEMENT', 1.00, 0.00, 0.00, '2024-01-01', TRUE),
('Kerala', 'LEAVE_LICENSE', 0.50, 0.00, 0.00, '2024-01-01', TRUE),
('Kerala', 'PG_AGREEMENT', 0.10, 0.00, 0.00, '2024-01-01', TRUE);

-- West Bengal
INSERT INTO stamp_duty_configs (state, agreement_type, duty_percent, registration_percent, surcharge_percent, effective_from, active) VALUES
('West Bengal', 'SALE_DEED', 6.00, 1.00, 0.00, '2024-01-01', TRUE),
('West Bengal', 'SALE_AGREEMENT', 0.10, 0.00, 0.00, '2024-01-01', TRUE),
('West Bengal', 'RENTAL_AGREEMENT', 0.25, 0.00, 0.00, '2024-01-01', TRUE),
('West Bengal', 'LEAVE_LICENSE', 0.25, 0.00, 0.00, '2024-01-01', TRUE),
('West Bengal', 'PG_AGREEMENT', 0.10, 0.00, 0.00, '2024-01-01', TRUE);

-- =====================
-- 10 Advocates across major cities
-- =====================
INSERT INTO advocates (full_name, phone, email, bar_council_number, specializations, city, state, cases_completed, rating, active) VALUES
('Adv. Rajesh Sharma', '9876543210', 'rajesh.sharma@lawfirm.in', 'MH/1234/2010', ARRAY['TITLE_SEARCH', 'SALE_DEED', 'DUE_DILIGENCE'], 'Mumbai', 'Maharashtra', 245, 4.80, TRUE),
('Adv. Priya Nair', '9876543211', 'priya.nair@lawfirm.in', 'KA/5678/2012', ARRAY['TITLE_SEARCH', 'DUE_DILIGENCE', 'RENTAL_AGREEMENT'], 'Bangalore', 'Karnataka', 189, 4.75, TRUE),
('Adv. Suresh Reddy', '9876543212', 'suresh.reddy@lawfirm.in', 'TS/9012/2011', ARRAY['SALE_DEED', 'TITLE_SEARCH', 'LITIGATION'], 'Hyderabad', 'Telangana', 312, 4.85, TRUE),
('Adv. Meena Krishnan', '9876543213', 'meena.k@lawfirm.in', 'TN/3456/2009', ARRAY['TITLE_SEARCH', 'DUE_DILIGENCE', 'RERA_COMPLIANCE'], 'Chennai', 'Tamil Nadu', 156, 4.70, TRUE),
('Adv. Amit Gupta', '9876543214', 'amit.gupta@lawfirm.in', 'DL/7890/2013', ARRAY['SALE_DEED', 'DUE_DILIGENCE', 'BUILDER_AGREEMENT'], 'New Delhi', 'Delhi', 278, 4.82, TRUE),
('Adv. Kavita Joshi', '9876543215', 'kavita.joshi@lawfirm.in', 'RJ/2345/2014', ARRAY['TITLE_SEARCH', 'RENTAL_AGREEMENT', 'LAND_DISPUTE'], 'Jaipur', 'Rajasthan', 134, 4.65, TRUE),
('Adv. Deepak Patel', '9876543216', 'deepak.patel@lawfirm.in', 'GJ/6789/2010', ARRAY['SALE_DEED', 'DUE_DILIGENCE', 'TITLE_SEARCH'], 'Ahmedabad', 'Gujarat', 198, 4.78, TRUE),
('Adv. Sunita Verma', '9876543217', 'sunita.verma@lawfirm.in', 'UP/0123/2011', ARRAY['TITLE_SEARCH', 'LITIGATION', 'ENCUMBRANCE'], 'Lucknow', 'Uttar Pradesh', 167, 4.60, TRUE),
('Adv. Arun Menon', '9876543218', 'arun.menon@lawfirm.in', 'KL/4567/2012', ARRAY['SALE_DEED', 'DUE_DILIGENCE', 'NRI_PROPERTY'], 'Kochi', 'Kerala', 143, 4.72, TRUE),
('Adv. Satabdi Roy', '9876543219', 'satabdi.roy@lawfirm.in', 'WB/8901/2013', ARRAY['TITLE_SEARCH', 'RENTAL_AGREEMENT', 'MUTATION'], 'Kolkata', 'West Bengal', 121, 4.55, TRUE);

-- =====================
-- 5 Interior Designers
-- =====================
INSERT INTO interior_designers (full_name, phone, email, city, experience_years, specializations, portfolio_urls, projects_completed, rating, active) VALUES
('Ananya Design Studio', '9988776601', 'ananya@designstudio.in', 'Mumbai', 12, ARRAY['FULL_HOME', 'MODULAR_KITCHEN', 'MODERN'], ARRAY['/portfolio/ananya/1.jpg', '/portfolio/ananya/2.jpg'], 87, 4.85, TRUE),
('Vastu Interiors', '9988776602', 'info@vastuinteriors.in', 'Bangalore', 8, ARRAY['FULL_HOME', 'WARDROBE', 'CONTEMPORARY'], ARRAY['/portfolio/vastu/1.jpg', '/portfolio/vastu/2.jpg'], 65, 4.72, TRUE),
('Decor Dreams', '9988776603', 'hello@decordreams.in', 'Hyderabad', 6, ARRAY['MODULAR_KITCHEN', 'FULL_ROOM', 'MINIMALIST'], ARRAY['/portfolio/decor/1.jpg', '/portfolio/decor/2.jpg'], 42, 4.68, TRUE),
('SpaceWise Designs', '9988776604', 'contact@spacewise.in', 'Chennai', 10, ARRAY['FULL_HOME', 'RENOVATION', 'TRADITIONAL'], ARRAY['/portfolio/spacewise/1.jpg', '/portfolio/spacewise/2.jpg'], 73, 4.80, TRUE),
('Urban Nest Interiors', '9988776605', 'team@urbannest.in', 'New Delhi', 15, ARRAY['FULL_HOME', 'MODULAR_KITCHEN', 'INDUSTRIAL'], ARRAY['/portfolio/urbannest/1.jpg', '/portfolio/urbannest/2.jpg'], 112, 4.90, TRUE);

-- =====================
-- 20 Materials Catalog entries
-- =====================
INSERT INTO materials_catalog (category, material_name, brand, finish, unit_price_paise, unit, image_url, specifications_json, active) VALUES
-- Flooring
('FLOORING', 'Italian Marble - Statuario', 'Imported', 'Polished', 45000000, 'SQFT', '/materials/statuario.jpg', '{"thickness_mm": 18, "origin": "Italy", "type": "Natural Stone"}', TRUE),
('FLOORING', 'Vitrified Tiles 800x800', 'Kajaria', 'Glossy', 8500000, 'SQFT', '/materials/kajaria-vit.jpg', '{"thickness_mm": 10, "size": "800x800mm", "type": "Vitrified"}', TRUE),
('FLOORING', 'Engineered Hardwood Oak', 'Pergo', 'Matte', 22000000, 'SQFT', '/materials/pergo-oak.jpg', '{"thickness_mm": 14, "wood": "Oak", "type": "Engineered"}', TRUE),
('FLOORING', 'Laminate Flooring', 'GreenLam', 'Wood Finish', 7500000, 'SQFT', '/materials/greenlam-lam.jpg', '{"thickness_mm": 8, "ac_rating": "AC4", "type": "Laminate"}', TRUE),

-- Wall
('WALL', 'Premium Emulsion Paint', 'Asian Paints Royale', 'Matte', 55000, 'SQFT', '/materials/royale-matt.jpg', '{"coverage_sqft_per_litre": 140, "coats": 2, "type": "Emulsion"}', TRUE),
('WALL', 'Textured Wall Paint', 'Dulux Velvet Touch', 'Satin', 72000, 'SQFT', '/materials/dulux-velvet.jpg', '{"coverage_sqft_per_litre": 120, "coats": 2, "type": "Textured"}', TRUE),
('WALL', 'Wallpaper - Designer', 'Nilaya', 'Textured', 15000000, 'SQFT', '/materials/nilaya-wp.jpg', '{"roll_width_inches": 21, "roll_length_feet": 33, "type": "Non-woven"}', TRUE),

-- Countertop
('COUNTERTOP', 'Granite - Black Galaxy', 'Natural', 'Polished', 35000000, 'SQFT', '/materials/black-galaxy.jpg', '{"thickness_mm": 20, "origin": "Andhra Pradesh", "type": "Granite"}', TRUE),
('COUNTERTOP', 'Quartz - Calacatta', 'Caesarstone', 'Polished', 55000000, 'SQFT', '/materials/caesarstone-cal.jpg', '{"thickness_mm": 20, "type": "Engineered Quartz"}', TRUE),
('COUNTERTOP', 'Solid Surface', 'Corian', 'Matte', 42000000, 'SQFT', '/materials/corian-white.jpg', '{"thickness_mm": 12, "seamless": true, "type": "Solid Surface"}', TRUE),

-- Cabinet
('CABINET', 'Modular Kitchen -?"L" Shaped', 'Hettich', 'Laminate', 175000000000, 'SET', '/materials/hettich-l.jpg', '{"material": "HDHMR", "hardware": "Hettich", "soft_close": true}', TRUE),
('CABINET', 'Sliding Wardrobe 7ft', 'Hafele', 'Lacquer', 125000000000, 'SET', '/materials/hafele-ward.jpg', '{"material": "MDF", "hardware": "Hafele", "mirror": true}', TRUE),
('CABINET', 'TV Unit with Storage', 'Custom', 'Veneer', 65000000000, 'UNIT', '/materials/tv-unit.jpg', '{"material": "Plywood", "length_feet": 8, "drawers": 4}', TRUE),

-- Hardware
('HARDWARE', 'Kitchen Basket Set (6pcs)', 'Hettich', 'SS 304', 2800000000, 'SET', '/materials/hettich-basket.jpg', '{"pieces": 6, "material": "SS 304", "soft_close": true}', TRUE),
('HARDWARE', 'Cabinet Hinges (pair)', 'Blum', 'Nickel', 85000000, 'SET', '/materials/blum-hinge.jpg', '{"type": "Soft Close", "angle": 110, "brand": "Blum"}', TRUE),

-- Lighting
('LIGHTING', 'LED Panel Light 18W', 'Philips', 'Warm White', 120000000, 'UNIT', '/materials/philips-panel.jpg', '{"wattage": 18, "color_temp": "3000K", "lumens": 1800}', TRUE),
('LIGHTING', 'Chandelier - Crystal', 'Imported', 'Chrome', 1500000000, 'UNIT', '/materials/crystal-chand.jpg', '{"diameter_inches": 24, "bulbs": 8, "type": "Crystal"}', TRUE),
('LIGHTING', 'Track Light 3-Spot', 'Havells', 'Black', 350000000, 'UNIT', '/materials/havells-track.jpg', '{"spots": 3, "wattage_per_spot": 7, "adjustable": true}', TRUE),

-- Paint
('PAINT', 'Exterior Weatherproof Paint', 'Asian Paints Apex', 'Smooth', 48000, 'SQFT', '/materials/apex-ext.jpg', '{"coverage_sqft_per_litre": 130, "warranty_years": 7, "type": "Exterior"}', TRUE),
('PAINT', 'Wood Polish - PU Finish', 'Shalimar', 'Glossy', 95000, 'SQFT', '/materials/shalimar-pu.jpg', '{"coats": 3, "drying_hours": 4, "type": "Polyurethane"}', TRUE);
