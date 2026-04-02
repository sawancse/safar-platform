-- Seed Apollo Hospitals branches + nearby stay listings + medical stay packages
-- Apollo is present in Chennai, Hyderabad, Delhi, Mumbai, Bangalore, Kolkata, Ahmedabad

DO $$
DECLARE
    host UUID := '00000000-0000-0000-0000-000000000001';

    -- Apollo Hospital IDs
    apollo_chennai   UUID := 'b0000000-0000-0000-0000-000000000001';
    apollo_hyderabad UUID := 'b0000000-0000-0000-0000-000000000002';
    apollo_delhi     UUID := 'b0000000-0000-0000-0000-000000000003';
    apollo_mumbai    UUID := 'b0000000-0000-0000-0000-000000000004';
    apollo_bangalore UUID := 'b0000000-0000-0000-0000-000000000005';
    apollo_kolkata   UUID := 'b0000000-0000-0000-0000-000000000006';
    apollo_ahmedabad UUID := 'b0000000-0000-0000-0000-000000000007';

    -- Listing IDs (near each Apollo)
    lst_chennai_1   UUID := 'c0000000-0000-0000-0000-000000000001';
    lst_chennai_2   UUID := 'c0000000-0000-0000-0000-000000000002';
    lst_hyderabad_1 UUID := 'c0000000-0000-0000-0000-000000000003';
    lst_hyderabad_2 UUID := 'c0000000-0000-0000-0000-000000000004';
    lst_delhi_1     UUID := 'c0000000-0000-0000-0000-000000000005';
    lst_delhi_2     UUID := 'c0000000-0000-0000-0000-000000000006';
    lst_mumbai_1    UUID := 'c0000000-0000-0000-0000-000000000007';
    lst_mumbai_2    UUID := 'c0000000-0000-0000-0000-000000000008';
    lst_bangalore_1 UUID := 'c0000000-0000-0000-0000-000000000009';
    lst_bangalore_2 UUID := 'c0000000-0000-0000-0000-00000000000a';
    lst_kolkata_1   UUID := 'c0000000-0000-0000-0000-00000000000b';
    lst_ahmedabad_1 UUID := 'c0000000-0000-0000-0000-00000000000c';

BEGIN

-- ═══════════════════════════════════════════════════════════════
-- 1. Apollo Hospital branches
-- ═══════════════════════════════════════════════════════════════

-- First update the existing Apollo Chennai entry if it exists (from V26)
-- Insert new Apollo branches
INSERT INTO listings.hospital_partners (id, name, city, address, lat, lng, specialties, accreditations, contact_email, active, rating, phone, website, description, airport_distance_km)
VALUES
(apollo_chennai, 'Apollo Hospitals Greams Road', 'Chennai',
 '21 Greams Lane, Off Greams Road, Chennai 600006',
 13.0604, 80.2496,
 'Cardiology,Orthopedics,Oncology,Neurology,Fertility,Cosmetic,Ophthalmology,Gastroenterology',
 'NABH,JCI,NABL',
 'info@apollochennai.com', true, 4.8, '+914428290200',
 'https://www.apollohospitals.com/chennai',
 'Apollo''s flagship hospital — India''s first corporate hospital. 60+ specialties, 700+ beds, internationally accredited.',
 15.0),

(apollo_hyderabad, 'Apollo Hospitals Jubilee Hills', 'Hyderabad',
 'Jubilee Hills, Hyderabad 500033',
 17.4260, 78.4070,
 'Cardiology,Orthopedics,Oncology,Neurology,Fertility,Transplant,Robotic Surgery',
 'NABH,JCI',
 'info@apollohyderabad.com', true, 4.7, '+914023607777',
 'https://www.apollohospitals.com/hyderabad',
 'Premier multi-specialty hospital with India''s first proton therapy centre and robotic surgery unit.',
 25.0),

(apollo_delhi, 'Apollo Hospitals Sarita Vihar', 'Delhi',
 'Sarita Vihar, Delhi-Mathura Road, New Delhi 110076',
 28.5355, 77.2870,
 'Cardiology,Orthopedics,Oncology,Neurology,Fertility,Transplant,Gastroenterology',
 'NABH,JCI',
 'info@apollodelhi.com', true, 4.7, '+911126925858',
 'https://www.apollohospitals.com/delhi',
 'North India''s leading Apollo facility with 710 beds, 52 specialties, and organ transplant programme.',
 18.0),

(apollo_mumbai, 'Apollo Hospitals Navi Mumbai', 'Mumbai',
 'Plot 13, Parsik Hill Rd, Sector 23, CBD Belapur, Navi Mumbai 400614',
 19.0190, 73.0370,
 'Cardiology,Orthopedics,Oncology,Neurology,Cosmetic,Dental,Ophthalmology',
 'NABH,JCI',
 'info@apollomumbai.com', true, 4.6, '+912227891000',
 'https://www.apollohospitals.com/mumbai',
 'Mumbai''s Apollo hub with advanced cardiac catheterization lab and da Vinci robotic surgery.',
 12.0),

(apollo_bangalore, 'Apollo Hospitals Bannerghatta', 'Bangalore',
 'No. 154/11, Bannerghatta Road, Bangalore 560076',
 12.8892, 77.5970,
 'Cardiology,Orthopedics,Oncology,Neurology,Fertility,Gastroenterology,Cosmetic',
 'NABH,JCI',
 'info@apollobangalore.com', true, 4.7, '+918026304050',
 'https://www.apollohospitals.com/bangalore',
 'Bangalore''s flagship Apollo with 250+ beds, CyberKnife radiosurgery, and comprehensive cancer centre.',
 30.0),

(apollo_kolkata, 'Apollo Gleneagles Hospital', 'Kolkata',
 '58, Canal Circular Road, Kolkata 700054',
 22.5180, 88.3690,
 'Cardiology,Orthopedics,Oncology,Neurology,Transplant,Gastroenterology',
 'NABH,JCI',
 'info@apollokolkata.com', true, 4.6, '+913323203040',
 'https://www.apollogleneagles.in',
 'Joint venture with Parkway Health — Eastern India''s most advanced multi-organ transplant centre.',
 16.0),

(apollo_ahmedabad, 'Apollo Hospitals Gandhinagar', 'Ahmedabad',
 'GIDC Estate, Gandhinagar 382010',
 23.2150, 72.6370,
 'Cardiology,Orthopedics,Oncology,Neurology,Fertility,Ophthalmology',
 'NABH',
 'info@apolloahmedabad.com', true, 4.5, '+917923268000',
 'https://www.apollohospitals.com/ahmedabad',
 'Western India''s Apollo centre with advanced joint replacement and IVF facilities.',
 10.0)
ON CONFLICT (id) DO NOTHING;


-- ═══════════════════════════════════════════════════════════════
-- 2. Procedures for Apollo hospitals
-- ═══════════════════════════════════════════════════════════════
INSERT INTO listings.hospital_procedures (id, hospital_id, procedure_name, specialty, est_cost_min_paise, est_cost_max_paise, hospital_days, recovery_days, success_rate, description)
VALUES
-- Chennai
(gen_random_uuid(), apollo_chennai, 'Bypass Surgery (CABG)', 'Cardiology', 25000000, 40000000, 7, 30, 97.8, 'Coronary artery bypass grafting by Apollo''s senior cardiac surgeons'),
(gen_random_uuid(), apollo_chennai, 'Total Knee Replacement', 'Orthopedics', 18000000, 30000000, 5, 42, 96.5, 'MAKOplasty robotic-assisted knee replacement'),
(gen_random_uuid(), apollo_chennai, 'Liver Transplant', 'Transplant', 150000000, 250000000, 14, 60, 92.0, 'Living & cadaveric liver transplant by India''s top team'),
(gen_random_uuid(), apollo_chennai, 'Proton Therapy', 'Oncology', 100000000, 200000000, 1, 30, 90.0, 'Advanced proton beam radiation for targeted cancer treatment'),

-- Hyderabad
(gen_random_uuid(), apollo_hyderabad, 'Robotic Prostatectomy', 'Oncology', 35000000, 55000000, 3, 21, 95.0, 'da Vinci robotic prostate cancer surgery'),
(gen_random_uuid(), apollo_hyderabad, 'IVF Cycle', 'Fertility', 12000000, 22000000, 1, 14, 48.0, 'Complete IVF cycle with Apollo Fertility specialists'),
(gen_random_uuid(), apollo_hyderabad, 'Angioplasty with Stent', 'Cardiology', 15000000, 28000000, 2, 14, 98.5, 'Minimally invasive coronary angioplasty'),

-- Delhi
(gen_random_uuid(), apollo_delhi, 'Bone Marrow Transplant', 'Oncology', 120000000, 200000000, 21, 90, 85.0, 'Autologous and allogeneic BMT for blood cancers'),
(gen_random_uuid(), apollo_delhi, 'Spine Fusion Surgery', 'Orthopedics', 30000000, 55000000, 5, 45, 94.0, 'Minimally invasive spine fusion for disc herniation'),
(gen_random_uuid(), apollo_delhi, 'LASIK', 'Ophthalmology', 4000000, 10000000, 0, 5, 99.2, 'Bladeless LASIK with Contoura Vision technology'),

-- Mumbai
(gen_random_uuid(), apollo_mumbai, 'Rhinoplasty', 'Cosmetic', 10000000, 22000000, 1, 14, 96.0, 'Cosmetic nose reshaping by board-certified plastic surgeons'),
(gen_random_uuid(), apollo_mumbai, 'Dental Implants (Full Mouth)', 'Dental', 15000000, 35000000, 1, 10, 97.5, 'Full-mouth All-on-4 dental implant restoration'),

-- Bangalore
(gen_random_uuid(), apollo_bangalore, 'CyberKnife Radiosurgery', 'Oncology', 80000000, 150000000, 0, 14, 91.0, 'Non-invasive stereotactic radiosurgery for tumours'),
(gen_random_uuid(), apollo_bangalore, 'Hip Replacement', 'Orthopedics', 22000000, 38000000, 5, 42, 95.8, 'Anterior approach total hip replacement'),

-- Kolkata
(gen_random_uuid(), apollo_kolkata, 'Kidney Transplant', 'Transplant', 80000000, 150000000, 10, 45, 95.0, 'Living-donor kidney transplant with ABO-incompatible protocol'),
(gen_random_uuid(), apollo_kolkata, 'Gastric Bypass', 'Gastroenterology', 30000000, 50000000, 3, 30, 93.0, 'Laparoscopic Roux-en-Y gastric bypass for weight loss'),

-- Ahmedabad
(gen_random_uuid(), apollo_ahmedabad, 'Cataract Surgery (Phaco)', 'Ophthalmology', 2500000, 6000000, 0, 3, 99.5, 'Micro-incision phacoemulsification with premium IOL'),
(gen_random_uuid(), apollo_ahmedabad, 'IUI Treatment', 'Fertility', 1500000, 3000000, 0, 7, 20.0, 'Intrauterine insemination fertility treatment');


-- ═══════════════════════════════════════════════════════════════
-- 3. Nearby stay listings (VERIFIED, medical_stay=true)
-- ═══════════════════════════════════════════════════════════════
INSERT INTO listings.listings (id, host_id, title, description, type, address_line1, city, state, pincode, lat, lng, max_guests, bedrooms, bathrooms, amenities, base_price_paise, pricing_unit, status, medical_stay)
VALUES
-- Chennai (near Apollo Greams Road — 13.0604, 80.2496)
(lst_chennai_1, host,
 'Recovery Suite near Apollo Chennai',
 'Fully furnished 2BHK apartment just 800m from Apollo Hospitals Greams Road. Ground floor, wheelchair accessible, equipped with hospital bed, oxygen concentrator, and 24/7 caretaker on call. Perfect for post-surgery recovery.',
 'HOME', '15 Nungambakkam High Road', 'Chennai', 'Tamil Nadu', '600034',
 13.0620, 80.2480, 4, 2, 2,
 ARRAY['WiFi','AC','Elevator','Wheelchair Accessible','Medical Equipment','Kitchen','Washing Machine','Power Backup','CCTV','Parking'],
 350000, 'NIGHT', 'VERIFIED', true),

(lst_chennai_2, host,
 'Caregiver-Friendly Stay at Greams Lane',
 'Spacious service apartment 500m from Apollo Hospital. Separate caregiver room, attached kitchen for dietary needs, nurse call button, and free hospital shuttle. South Indian & diet meals available.',
 'HOME', '8A Greams Lane', 'Chennai', 'Tamil Nadu', '600006',
 13.0598, 80.2505, 6, 3, 2,
 ARRAY['WiFi','AC','Kitchen','Caregiver Room','Hospital Shuttle','Nurse Call','Diet Meals','Wheelchair Accessible','Laundry','Parking'],
 450000, 'NIGHT', 'VERIFIED', true),

-- Hyderabad (near Apollo Jubilee Hills — 17.4260, 78.4070)
(lst_hyderabad_1, host,
 'Healing Retreat near Apollo Hyderabad',
 'Peaceful 1BHK apartment in Jubilee Hills, 1.2 km from Apollo Hospital. Fully air-conditioned, recliner bed, attached balcony with garden view. Complimentary hospital pickup daily.',
 'HOME', '45 Road No 36, Jubilee Hills', 'Hyderabad', 'Telangana', '500033',
 17.4275, 78.4050, 3, 1, 1,
 ARRAY['WiFi','AC','Balcony','Garden View','Hospital Pickup','Recliner Bed','Kitchen','Power Backup','Elevator'],
 280000, 'NIGHT', 'VERIFIED', true),

(lst_hyderabad_2, host,
 'Apollo-Adjacent Family Suite Hyderabad',
 'Large 3BHK for patient families — walking distance to Apollo Jubilee Hills. Kids room, kitchen stocked with basics, laundry, and a dedicated recovery room with adjustable bed.',
 'HOME', '22 Film Nagar, Jubilee Hills', 'Hyderabad', 'Telangana', '500096',
 17.4245, 78.4090, 8, 3, 3,
 ARRAY['WiFi','AC','Kitchen','Kids Room','Recovery Room','Adjustable Bed','Washing Machine','Parking','CCTV','Power Backup'],
 520000, 'NIGHT', 'VERIFIED', true),

-- Delhi (near Apollo Sarita Vihar — 28.5355, 77.2870)
(lst_delhi_1, host,
 'Post-Op Haven near Apollo Delhi',
 'Modern studio apartment 600m from Apollo Sarita Vihar. Hospital-grade mattress, air purifier, attached bathroom with grab bars, and complimentary meals service. Ideal for single patients.',
 'ROOM', '12 Jasola Vihar', 'Delhi', 'Delhi', '110025',
 28.5370, 77.2855, 2, 1, 1,
 ARRAY['WiFi','AC','Air Purifier','Grab Bars','Hospital Bed','Meals Service','Elevator','CCTV','Power Backup'],
 250000, 'NIGHT', 'VERIFIED', true),

(lst_delhi_2, host,
 'Medical Stay Apartment Apollo Delhi',
 'Spacious 2BHK near Apollo Hospital Delhi with dedicated nursing support available. Fully equipped kitchen for special diets, wheelchair ramp, and free pharmacy delivery.',
 'HOME', '5B Sarita Vihar Main Road', 'Delhi', 'Delhi', '110076',
 28.5340, 77.2890, 5, 2, 2,
 ARRAY['WiFi','AC','Kitchen','Wheelchair Ramp','Pharmacy Delivery','Nursing Support','Washing Machine','Parking','Power Backup','CCTV'],
 400000, 'NIGHT', 'VERIFIED', true),

-- Mumbai (near Apollo Navi Mumbai — 19.0190, 73.0370)
(lst_mumbai_1, host,
 'Apollo Belapur Recovery Flat',
 'Bright 1BHK flat in CBD Belapur, 800m from Apollo Hospital. Sea breeze, quiet neighbourhood, medical fridge, and attached nurse quarter. Steps from station for visitor convenience.',
 'HOME', '15 Palm Beach Road, Sector 25', 'Mumbai', 'Maharashtra', '400614',
 19.0205, 73.0385, 3, 1, 1,
 ARRAY['WiFi','AC','Sea View','Medical Fridge','Nurse Quarter','Near Station','Elevator','CCTV','Parking'],
 320000, 'NIGHT', 'VERIFIED', true),

(lst_mumbai_2, host,
 'Family Medical Stay Navi Mumbai',
 'Comfortable 3BHK apartment for patient families. 1 km from Apollo Navi Mumbai, rooftop garden for fresh air, full kitchen, and dedicated post-surgery recovery room.',
 'HOME', '8 Sector 23 CHS, CBD Belapur', 'Mumbai', 'Maharashtra', '400614',
 19.0175, 73.0355, 7, 3, 2,
 ARRAY['WiFi','AC','Rooftop Garden','Kitchen','Recovery Room','Wheelchair Accessible','Washing Machine','Power Backup','Parking'],
 480000, 'NIGHT', 'VERIFIED', true),

-- Bangalore (near Apollo Bannerghatta — 12.8892, 77.5970)
(lst_bangalore_1, host,
 'Green Recovery Home near Apollo Bangalore',
 'Villa-style 2BHK in JP Nagar, 1.5 km from Apollo Bannerghatta Road. Private garden, Ayurvedic massage on request, oxygen concentrator, and daily housekeeping.',
 'HOME', '45 JP Nagar 6th Phase', 'Bangalore', 'Karnataka', '560078',
 12.8910, 77.5955, 4, 2, 2,
 ARRAY['WiFi','AC','Private Garden','Ayurvedic Massage','Oxygen Concentrator','Housekeeping','Kitchen','Parking','Power Backup'],
 380000, 'NIGHT', 'VERIFIED', true),

(lst_bangalore_2, host,
 'Apollo Bangalore Studio Stay',
 'Compact studio apartment perfect for single patients. 900m walk to Apollo Hospital, attached kitchenette, grab bars, and free shuttle to hospital twice daily.',
 'ROOM', '12 BTM Layout 2nd Stage', 'Bangalore', 'Karnataka', '560076',
 12.8880, 77.5990, 2, 1, 1,
 ARRAY['WiFi','AC','Kitchenette','Grab Bars','Hospital Shuttle','Elevator','CCTV','Power Backup'],
 220000, 'NIGHT', 'VERIFIED', true),

-- Kolkata (near Apollo Gleneagles — 22.5180, 88.3690)
(lst_kolkata_1, host,
 'Apollo Kolkata Patient Home',
 'Well-maintained 2BHK flat on Canal Circular Road, 700m from Apollo Gleneagles. Bengali home-cooked meals available, caretaker room, and pharmacy within building complex.',
 'HOME', '62 Canal Circular Road', 'Kolkata', 'West Bengal', '700054',
 22.5195, 88.3705, 5, 2, 2,
 ARRAY['WiFi','AC','Home Meals','Caretaker Room','Pharmacy','Elevator','Kitchen','Washing Machine','Power Backup'],
 260000, 'NIGHT', 'VERIFIED', true),

-- Ahmedabad (near Apollo Gandhinagar — 23.2150, 72.6370)
(lst_ahmedabad_1, host,
 'Apollo Gandhinagar Recovery Stay',
 'Modern 1BHK near Apollo Hospitals Gandhinagar. Jain/Gujarati vegetarian meals, wheelchair accessible, air-conditioned, with complimentary airport pickup for international patients.',
 'HOME', '15 GIDC Road, Sector 26', 'Ahmedabad', 'Gujarat', '382010',
 23.2165, 72.6385, 3, 1, 1,
 ARRAY['WiFi','AC','Vegetarian Meals','Wheelchair Accessible','Airport Pickup','Kitchen','Parking','Power Backup','CCTV'],
 200000, 'NIGHT', 'VERIFIED', true);


-- ═══════════════════════════════════════════════════════════════
-- 4. Medical stay packages (linking listings ↔ Apollo hospitals)
-- ═══════════════════════════════════════════════════════════════
INSERT INTO listings.medical_stay_packages (id, listing_id, hospital_id, distance_km, includes_pickup, includes_translator, caregiver_friendly, medical_price_paise, min_stay_nights, recovery_days)
VALUES
-- Chennai
(gen_random_uuid(), lst_chennai_1, apollo_chennai, 0.80, true, true, true, 350000, 3, 7),
(gen_random_uuid(), lst_chennai_2, apollo_chennai, 0.50, true, true, true, 450000, 3, 14),

-- Hyderabad
(gen_random_uuid(), lst_hyderabad_1, apollo_hyderabad, 1.20, true, false, false, 280000, 3, 7),
(gen_random_uuid(), lst_hyderabad_2, apollo_hyderabad, 1.50, true, true, true, 520000, 5, 14),

-- Delhi
(gen_random_uuid(), lst_delhi_1, apollo_delhi, 0.60, false, false, false, 250000, 2, 5),
(gen_random_uuid(), lst_delhi_2, apollo_delhi, 0.90, true, true, true, 400000, 3, 10),

-- Mumbai
(gen_random_uuid(), lst_mumbai_1, apollo_mumbai, 0.80, true, true, true, 320000, 3, 7),
(gen_random_uuid(), lst_mumbai_2, apollo_mumbai, 1.00, true, true, true, 480000, 5, 14),

-- Bangalore
(gen_random_uuid(), lst_bangalore_1, apollo_bangalore, 1.50, true, false, true, 380000, 3, 10),
(gen_random_uuid(), lst_bangalore_2, apollo_bangalore, 0.90, true, false, false, 220000, 2, 5),

-- Kolkata
(gen_random_uuid(), lst_kolkata_1, apollo_kolkata, 0.70, true, true, true, 260000, 3, 7),

-- Ahmedabad
(gen_random_uuid(), lst_ahmedabad_1, apollo_ahmedabad, 0.80, true, true, true, 200000, 3, 7);

END $$;
