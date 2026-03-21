-- Seed hospital partners
INSERT INTO listings.hospital_partners (id, name, city, address, lat, lng, specialties, accreditations, contact_email, active, rating, phone, website, description, airport_distance_km)
VALUES
  (gen_random_uuid(), 'Apollo Hospitals', 'Chennai', '21 Greams Lane, Off Greams Road', 13.0604, 80.2496, 'Cardiology,Orthopedics,Oncology,Neurology,Fertility', 'NABH,JCI', 'info@apollohospitals.com', true, 4.8, '+914428290200', 'https://www.apollohospitals.com', 'India''s largest hospital chain with world-class facilities for international patients', 12.5),
  (gen_random_uuid(), 'Fortis Hospital', 'Mumbai', 'Mulund Goregaon Link Rd, Mulund West', 19.1726, 72.9425, 'Cardiology,Orthopedics,Cosmetic,Dental', 'NABH,ISO', 'info@fortishealthcare.com', true, 4.6, '+912225991000', 'https://www.fortishealthcare.com', 'Premier multi-specialty hospital with advanced cardiac and orthopedic care', 18.0),
  (gen_random_uuid(), 'Medanta - The Medicity', 'Delhi', 'CH Baktawar Singh Rd, Sector 38, Gurugram', 28.4421, 77.0420, 'Cardiology,Neurology,Oncology,Fertility,Cosmetic', 'NABH,JCI', 'info@medanta.org', true, 4.7, '+911244141414', 'https://www.medanta.org', 'World-renowned hospital founded by Dr. Naresh Trehan, known for cardiac surgery', 15.0),
  (gen_random_uuid(), 'Manipal Hospital', 'Bangalore', '98 HAL Old Airport Rd, Kodihalli', 12.9608, 77.6478, 'Orthopedics,Oncology,Neurology,Ophthalmology', 'NABH', 'info@manipalhospitals.com', true, 4.5, '+918025023456', 'https://www.manipalhospitals.com', 'Leading healthcare provider in South India with cutting-edge technology', 8.0),
  (gen_random_uuid(), 'AIIMS', 'Delhi', 'Ansari Nagar East, New Delhi', 28.5672, 77.2100, 'Cardiology,Neurology,Oncology,Ophthalmology,Dental', 'NABH', 'director@aiims.edu', true, 4.9, '+911126588500', 'https://www.aiims.edu', 'India''s premier public hospital and research institution', 20.0),
  (gen_random_uuid(), 'Narayana Health', 'Bangalore', '258/A Bommasandra Industrial Area, Hosur Road', 12.8089, 77.6947, 'Cardiology,Orthopedics,Fertility,Ayurveda', 'NABH,JCI', 'info@narayanahealth.org', true, 4.6, '+918071222222', 'https://www.narayanahealth.org', 'Affordable world-class healthcare, known for cardiac surgery at low cost', 22.0),
  (gen_random_uuid(), 'Amrita Hospital', 'Kochi', 'AIMS Ponekkara PO, Kochi', 10.0390, 76.3120, 'Cardiology,Oncology,Neurology,Ayurveda', 'NABH', 'info@amritahospital.org', true, 4.5, '+914842801234', 'https://www.amritahospitals.org', 'Charitable hospital with advanced research in traditional and modern medicine', 30.0),
  (gen_random_uuid(), 'Max Super Speciality Hospital', 'Delhi', '1, Press Enclave Road, Saket', 28.5244, 77.2116, 'Cardiology,Cosmetic,Dental,Ophthalmology,Fertility', 'NABH,JCI', 'info@maxhealthcare.com', true, 4.6, '+911126515050', 'https://www.maxhealthcare.in', 'Premium healthcare with state-of-the-art cosmetic and dental surgery centers', 14.0),
  (gen_random_uuid(), 'CMC Vellore', 'Vellore', 'Ida Scudder Rd, Vellore', 12.9249, 79.1325, 'Orthopedics,Neurology,Ophthalmology,Fertility', 'NABH', 'cmcinfo@cmcvellore.ac.in', true, 4.8, '+914162282010', 'https://www.cmch-vellore.edu', 'One of India''s oldest and most trusted hospitals, known for complex surgeries', 25.0),
  (gen_random_uuid(), 'Kokilaben Hospital', 'Mumbai', 'Rao Saheb Achutrao Patwardhan Marg, Four Bungalows', 19.1294, 72.8284, 'Oncology,Neurology,Cosmetic,Dental', 'NABH,JCI', 'info@kokilabenhospital.com', true, 4.7, '+912230999999', 'https://www.kokilabenhospital.com', 'Advanced oncology and neuroscience center in the heart of Mumbai', 10.0);

-- Seed procedures for hospitals (use subquery to get hospital IDs)
INSERT INTO listings.hospital_procedures (id, hospital_id, procedure_name, specialty, est_cost_min_paise, est_cost_max_paise, hospital_days, recovery_days, success_rate, description)
SELECT gen_random_uuid(), h.id, p.procedure_name, p.specialty, p.min_cost, p.max_cost, p.h_days, p.r_days, p.rate, p.descr
FROM listings.hospital_partners h
CROSS JOIN (VALUES
  ('Bypass Surgery', 'Cardiology', 25000000, 45000000, 7, 30, 97.5, 'Coronary artery bypass grafting (CABG) to restore blood flow'),
  ('Angioplasty', 'Cardiology', 15000000, 30000000, 2, 14, 98.0, 'Minimally invasive procedure to open blocked arteries'),
  ('Knee Replacement', 'Orthopedics', 20000000, 35000000, 5, 45, 96.0, 'Total knee arthroplasty for severe arthritis or injury'),
  ('Hip Replacement', 'Orthopedics', 25000000, 40000000, 5, 42, 95.5, 'Total hip arthroplasty for hip joint damage'),
  ('Spine Surgery', 'Orthopedics', 30000000, 60000000, 5, 60, 93.0, 'Surgical treatment for spinal cord conditions'),
  ('LASIK', 'Ophthalmology', 5000000, 12000000, 0, 7, 99.0, 'Laser eye surgery for vision correction'),
  ('Cataract Surgery', 'Ophthalmology', 3000000, 8000000, 0, 3, 99.5, 'Removal and replacement of cloudy lens'),
  ('IVF', 'Fertility', 15000000, 25000000, 1, 14, 45.0, 'In-vitro fertilization treatment cycle'),
  ('Hair Transplant', 'Cosmetic', 8000000, 20000000, 0, 14, 95.0, 'FUE/FUT hair restoration procedure'),
  ('Dental Implants', 'Dental', 3000000, 10000000, 0, 7, 98.0, 'Permanent tooth replacement with titanium implants'),
  ('Rhinoplasty', 'Cosmetic', 10000000, 25000000, 1, 21, 96.0, 'Nose reshaping surgery for cosmetic or functional purposes'),
  ('Chemotherapy', 'Oncology', 10000000, 50000000, 3, 90, 70.0, 'Cancer treatment using anti-cancer drugs'),
  ('Panchakarma', 'Ayurveda', 5000000, 15000000, 7, 21, 85.0, 'Traditional Ayurvedic detox and rejuvenation therapy')
) AS p(procedure_name, specialty, min_cost, max_cost, h_days, r_days, rate, descr)
WHERE h.specialties LIKE '%' || p.specialty || '%';
