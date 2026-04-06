-- ── Seed Sale Properties (15 realistic Indian listings) ──────────────────

INSERT INTO sale_properties (seller_id, seller_type, title, description, sale_property_type, transaction_type,
  locality, city, state, pincode, lat, lng, asking_price_paise, price_per_sqft_paise, price_negotiable,
  carpet_area_sqft, built_up_area_sqft, bedrooms, bathrooms, balconies, floor_number, total_floors,
  facing, property_age_years, furnishing, parking_covered, parking_open, possession_status,
  amenities, water_supply, power_backup, gated_community, status)
VALUES
-- Hyderabad
('9b563ce0-a97d-4cfa-a103-da0db783557e', 'OWNER', 'Spacious 3 BHK in Gachibowli', 'Well-maintained apartment near IT corridor with lake view. Close to DLF, TCS, Infosys campuses. 24/7 security and power backup.', 'APARTMENT', 'RESALE',
 'Gachibowli', 'Hyderabad', 'Telangana', '500032', 17.4400, 78.3489, 1250000000, 850000, true,
 1450, 1800, 3, 3, 2, 12, 22, 'EAST', 5, 'SEMI_FURNISHED', 1, 0, 'READY_TO_MOVE',
 ARRAY['Swimming Pool','Gym','Clubhouse','Power Backup','Lift'], 'BOREWELL', 'FULL', true, 'ACTIVE'),

('9b563ce0-a97d-4cfa-a103-da0db783557e', 'OWNER', 'Premium 4 BHK Villa Jubilee Hills', 'Luxury independent villa in the heart of Jubilee Hills. Double-height living room, private garden, vastu compliant. Walking distance to GVK One Mall.', 'VILLA', 'RESALE',
 'Jubilee Hills', 'Hyderabad', 'Telangana', '500033', 17.4308, 78.4103, 4500000000, 1200000, true,
 3500, 4200, 4, 5, 3, 0, 3, 'NORTH', 8, 'FURNISHED', 2, 1, 'READY_TO_MOVE',
 ARRAY['Garden','Swimming Pool','Home Theatre','Servant Room','Generator'], 'CORPORATION', 'FULL', true, 'ACTIVE'),

('9b563ce0-a97d-4cfa-a103-da0db783557e', 'AGENT', 'Affordable 2 BHK Kondapur', 'Budget-friendly apartment in prime IT location. Close to Hitec City metro, hospitals, and schools.', 'APARTMENT', 'RESALE',
 'Kondapur', 'Hyderabad', 'Telangana', '500084', 17.4600, 78.3534, 650000000, 680000, true,
 950, 1150, 2, 2, 1, 7, 14, 'WEST', 3, 'UNFURNISHED', 1, 0, 'READY_TO_MOVE',
 ARRAY['Lift','Power Backup','Security','Children Play Area'], 'BOREWELL', 'PARTIAL', true, 'ACTIVE'),

-- Bangalore
('259ec18a-b45b-406b-b57c-9695bdcdf469', 'OWNER', 'Modern 3 BHK Whitefield', 'Newly built apartment in prestigious gated community. Excellent connectivity to ITPL and Marathahalli.', 'APARTMENT', 'NEW_BOOKING',
 'Whitefield', 'Bangalore', 'Karnataka', '560066', 12.9698, 77.7500, 1450000000, 920000, false,
 1550, 1900, 3, 3, 2, 15, 28, 'SOUTH', 0, 'SEMI_FURNISHED', 1, 1, 'UNDER_CONSTRUCTION',
 ARRAY['Swimming Pool','Gym','Clubhouse','Tennis Court','Jogging Track','CCTV'], 'CORPORATION', 'FULL', true, 'ACTIVE'),

('259ec18a-b45b-406b-b57c-9695bdcdf469', 'OWNER', 'Cozy 2 BHK HSR Layout', 'Perfect for young professionals. Walking distance to cafes, restaurants, and Agara Lake. Well-connected to Silk Board and Outer Ring Road.', 'APARTMENT', 'RESALE',
 'HSR Layout', 'Bangalore', 'Karnataka', '560102', 12.9116, 77.6370, 950000000, 850000, true,
 1100, 1350, 2, 2, 1, 4, 8, 'EAST', 4, 'SEMI_FURNISHED', 1, 0, 'READY_TO_MOVE',
 ARRAY['Gym','Security','Power Backup','Parking','Children Play Area'], 'CORPORATION', 'PARTIAL', true, 'ACTIVE'),

-- Mumbai
('55814843-9ce2-4bdd-ace4-30d8d0e98e36', 'OWNER', 'Sea-View 2 BHK Worli', 'Stunning Arabian Sea view from 32nd floor. Premium locality with easy access to Bandra-Worli Sea Link. Reputed builder construction.', 'APARTMENT', 'RESALE',
 'Worli', 'Mumbai', 'Maharashtra', '400018', 19.0176, 72.8152, 5500000000, 3500000, true,
 1050, 1400, 2, 2, 1, 32, 45, 'WEST', 6, 'FURNISHED', 1, 0, 'READY_TO_MOVE',
 ARRAY['Swimming Pool','Gym','Concierge','Valet Parking','Sea View','Clubhouse'], 'CORPORATION', 'FULL', true, 'ACTIVE'),

('55814843-9ce2-4bdd-ace4-30d8d0e98e36', 'AGENT', 'Spacious 1 BHK Andheri West', 'Well-connected to Western Express Highway. Walking distance to Andheri metro and Lokhandwala market.', 'APARTMENT', 'RESALE',
 'Andheri', 'Mumbai', 'Maharashtra', '400058', 19.1190, 72.8460, 1350000000, 2100000, true,
 580, 750, 1, 1, 1, 8, 15, 'NORTH', 10, 'SEMI_FURNISHED', 0, 0, 'READY_TO_MOVE',
 ARRAY['Lift','Security','Power Backup','Water Tank'], 'CORPORATION', 'PARTIAL', false, 'ACTIVE'),

-- Delhi NCR
('313557b0-ddb1-4249-a12b-5c46e9b210a2', 'BUILDER', 'Luxury 4 BHK Dwarka Expressway', 'Ultra-modern apartment on Dwarka Expressway. Branded fittings, modular kitchen, smart home features. RERA approved.', 'APARTMENT', 'NEW_BOOKING',
 'Dwarka', 'Delhi', 'Delhi', '110075', 28.5921, 77.0413, 2800000000, 1100000, false,
 2500, 3200, 4, 4, 3, 18, 32, 'SOUTH', 0, 'UNDER_CONSTRUCTION', 1, 1, 'UNDER_CONSTRUCTION',
 ARRAY['Swimming Pool','Gym','Clubhouse','Tennis Court','Spa','Rooftop Garden','Smart Home'], 'CORPORATION', 'FULL', true, 'ACTIVE'),

('313557b0-ddb1-4249-a12b-5c46e9b210a2', 'OWNER', 'Budget 2 BHK Noida Sector 75', 'Value for money apartment in Noida. Close to Sector 18 market, Atta Market, and Noida City Centre metro.', 'APARTMENT', 'RESALE',
 'Sector 75', 'Noida', 'Uttar Pradesh', '201301', 28.5805, 77.3905, 550000000, 580000, true,
 950, 1200, 2, 2, 1, 6, 18, 'EAST', 7, 'UNFURNISHED', 1, 0, 'READY_TO_MOVE',
 ARRAY['Gym','Swimming Pool','Security','Children Play Area','Power Backup'], 'BOREWELL', 'PARTIAL', true, 'ACTIVE'),

-- Pune
('433103c0-d51e-4de7-aa67-5819a5b7181f', 'OWNER', 'Hill-View 3 BHK Baner', 'Beautiful apartment overlooking Baner hills. Premium society with world-class amenities. 10 min from Hinjewadi IT Park.', 'APARTMENT', 'RESALE',
 'Baner', 'Pune', 'Maharashtra', '411045', 18.5596, 73.7871, 1100000000, 780000, true,
 1400, 1700, 3, 2, 2, 10, 16, 'NORTH_EAST', 4, 'SEMI_FURNISHED', 1, 0, 'READY_TO_MOVE',
 ARRAY['Swimming Pool','Gym','Clubhouse','Garden','Jogging Track','Children Play Area'], 'BOREWELL', 'FULL', true, 'ACTIVE'),

-- Chennai
('876d00da-c842-44db-a534-640480f98284', 'OWNER', 'Elegant 3 BHK OMR Sholinganallur', 'Premium apartment on IT Expressway. Siruseri IT Park, ELCOT SEZ nearby. Excellent schools and hospitals.', 'APARTMENT', 'NEW_BOOKING',
 'Sholinganallur', 'Chennai', 'Tamil Nadu', '600119', 12.9016, 80.2276, 980000000, 720000, false,
 1350, 1650, 3, 2, 2, 8, 20, 'SOUTH', 1, 'SEMI_FURNISHED', 1, 0, 'READY_TO_MOVE',
 ARRAY['Swimming Pool','Gym','Power Backup','Rainwater Harvesting','Solar Panels'], 'CORPORATION', 'FULL', true, 'ACTIVE'),

-- Jaipur
('dfa2f343-4ea1-4a41-bb56-1c65553b47e6', 'OWNER', 'Haveli-Style 5 BHK Vaishali Nagar', 'Unique blend of Rajasthani heritage and modern living. Huge courtyard, rooftop terrace, marble flooring.', 'INDEPENDENT_HOUSE', 'RESALE',
 'Vaishali Nagar', 'Jaipur', 'Rajasthan', '302021', 26.9154, 75.7229, 1800000000, 550000, true,
 3200, 4000, 5, 4, 2, 0, 3, 'EAST', 12, 'FURNISHED', 2, 2, 'READY_TO_MOVE',
 ARRAY['Garden','Servant Room','Study Room','Pooja Room','Rooftop Terrace'], 'BOREWELL', 'FULL', false, 'ACTIVE'),

-- Goa
('7c63eb91-53ea-44eb-8029-04335aca8f47', 'OWNER', 'Beach Villa 3 BHK Anjuna', 'Stunning Portuguese-style villa just 500m from Anjuna Beach. Perfect holiday home or rental investment. Rental income potential ₹2L/month.', 'VILLA', 'RESALE',
 'Anjuna', 'Goa', 'Goa', '403509', 15.5739, 73.7413, 3200000000, 950000, true,
 2800, 3500, 3, 3, 2, 0, 2, 'WEST', 6, 'FURNISHED', 1, 2, 'READY_TO_MOVE',
 ARRAY['Garden','Swimming Pool','BBQ Area','Rooftop Deck','Beach Access'], 'BOREWELL', 'FULL', false, 'ACTIVE'),

-- Kolkata
('259ec18a-b45b-406b-b57c-9695bdcdf469', 'OWNER', 'Modern 2 BHK New Town Rajarhat', 'Well-planned apartment in Kolkata new township. Metro connectivity, IT Hub proximity. Great appreciation potential.', 'APARTMENT', 'NEW_BOOKING',
 'New Town', 'Kolkata', 'West Bengal', '700156', 22.5807, 88.4790, 450000000, 520000, false,
 850, 1050, 2, 2, 1, 11, 22, 'SOUTH', 0, 'UNDER_CONSTRUCTION', 1, 0, 'UNDER_CONSTRUCTION',
 ARRAY['Swimming Pool','Gym','Clubhouse','CCTV','Power Backup','Rainwater Harvesting'], 'CORPORATION', 'FULL', true, 'ACTIVE'),

-- Ahmedabad
('55814843-9ce2-4bdd-ace4-30d8d0e98e36', 'OWNER', 'Premium Plot SG Highway', 'NA-approved residential plot on SG Highway. Clear title, ready for construction. Near GIFT City.', 'PLOT', 'RESALE',
 'SG Highway', 'Ahmedabad', 'Gujarat', '380054', 23.0430, 72.5290, 780000000, 1200000, true,
 null, null, null, null, null, null, null, null, null, null, 0, 0, 'READY_TO_MOVE',
 ARRAY[]::text[], 'BOREWELL', null, false, 'ACTIVE');

-- ── Seed Builder Projects (8 Indian projects) ────────────────────────────

INSERT INTO builder_projects (builder_id, builder_name, project_name, tagline, description,
  rera_id, rera_verified, city, state, locality, pincode, lat, lng,
  total_units, available_units, total_towers, total_floors_max,
  project_status, launch_date, possession_date, construction_progress_percent,
  amenities, min_price_paise, max_price_paise, min_bhk, max_bhk,
  min_area_sqft, max_area_sqft, status, verified,
  bank_approvals)
VALUES
('313557b0-ddb1-4249-a12b-5c46e9b210a2', 'Godrej Properties', 'Godrej Platinum Towers', 'Live the Platinum Life',
 'Ultra-luxury residential project in the heart of Gachibowli. Premium 3 & 4 BHK apartments with world-class amenities, infinity pool, and panoramic city views.',
 'P02200049382', true, 'Hyderabad', 'Telangana', 'Gachibowli', '500032', 17.4400, 78.3489,
 420, 185, 4, 35, 'UNDER_CONSTRUCTION', '2025-06-01', '2027-12-31', 45,
 ARRAY['Swimming Pool','Gym','Clubhouse','Tennis Court','Jogging Track','Spa','Home Theatre','Rooftop Garden','EV Charging','Smart Home'],
 1250000000, 4500000000, 2, 4, 1200, 3500, 'ACTIVE', true,
 ARRAY['SBI','HDFC','ICICI','Axis','Kotak']),

('259ec18a-b45b-406b-b57c-9695bdcdf469', 'Prestige Group', 'Prestige City Sarjapur', 'The Address of Success',
 'Integrated township with apartments, villas, and plots. Over 100 acres with schools, hospitals, shopping mall, and sports complex.',
 'PRM/KA/RERA/1251/304/PR/241119/002977', true, 'Bangalore', 'Karnataka', 'Sarjapur Road', '562125', 12.8560, 77.7870,
 5000, 3200, 18, 28, 'UNDER_CONSTRUCTION', '2024-01-15', '2028-06-30', 30,
 ARRAY['Swimming Pool','Gym','Clubhouse','Golf Course','Shopping Mall','School','Hospital','Jogging Track','Amphitheatre','Lake View'],
 4500000000, 18000000000, 1, 5, 650, 5000, 'ACTIVE', true,
 ARRAY['SBI','HDFC','ICICI','LIC','PNB','Canara']),

('55814843-9ce2-4bdd-ace4-30d8d0e98e36', 'Lodha Group', 'Lodha Park Worli', 'Where Mumbai Meets the Sky',
 'Super-luxury towers in Worli with unmatched sea views. Sky lobbies, private pools, concierge services. Indias tallest residential towers.',
 'P51900024589', true, 'Mumbai', 'Maharashtra', 'Worli', '400018', 19.0176, 72.8152,
 300, 80, 3, 75, 'UNDER_CONSTRUCTION', '2023-03-01', '2027-03-31', 60,
 ARRAY['Infinity Pool','Sky Lounge','Private Cinema','Concierge','Helipad','Wine Cellar','Spa','Business Centre','Valet Parking'],
 10000000000, 50000000000, 2, 5, 1800, 8000, 'ACTIVE', true,
 ARRAY['SBI','HDFC','ICICI','Kotak','Deutsche Bank']),

('313557b0-ddb1-4249-a12b-5c46e9b210a2', 'DLF Limited', 'DLF One Midtown', 'The New Centre of Delhi',
 'Premium residential project on Shivaji Marg, New Delhi. Smart-tech enabled apartments with DLF signature quality.',
 'DLREG/UP/2023/005436', true, 'Delhi', 'Delhi', 'Moti Nagar', '110015', 28.6592, 77.1506,
 800, 450, 6, 32, 'UNDER_CONSTRUCTION', '2024-06-01', '2028-12-31', 25,
 ARRAY['Swimming Pool','Gym','Clubhouse','Tennis Court','Squash Court','Rooftop Garden','Co-working Space','EV Charging'],
 2500000000, 8000000000, 2, 4, 1100, 3800, 'ACTIVE', true,
 ARRAY['SBI','HDFC','ICICI','Axis','PNB','BOB']),

('433103c0-d51e-4de7-aa67-5819a5b7181f', 'Shapoorji Pallonji', 'Joyville Hinjewadi', 'Joy of Spacious Living',
 'Affordable luxury in Pune IT corridor. 2 & 3 BHK apartments with zero-waste campus, solar-powered common areas, and EV-ready parking.',
 'P52100048290', true, 'Pune', 'Maharashtra', 'Hinjewadi', '411057', 18.5912, 73.7390,
 1200, 800, 8, 24, 'UNDER_CONSTRUCTION', '2024-09-01', '2027-09-30', 35,
 ARRAY['Swimming Pool','Gym','Clubhouse','Jogging Track','Amphitheatre','Co-working Space','EV Charging','Solar Panels','Rainwater Harvesting'],
 550000000, 1200000000, 2, 3, 900, 1650, 'ACTIVE', true,
 ARRAY['SBI','HDFC','ICICI','Axis','Kotak','LIC']),

('876d00da-c842-44db-a534-640480f98284', 'Casagrand', 'Casagrand First City', 'Your First Address in Chennai',
 'Mega township on GST Road near Mahindra World City. Affordable 1, 2 & 3 BHK apartments with world-class infrastructure.',
 'TN/01/Building/0099/2024', true, 'Chennai', 'Tamil Nadu', 'Sholinganallur', '603202', 12.7680, 80.1980,
 2500, 1800, 12, 18, 'UNDER_CONSTRUCTION', '2024-03-01', '2027-06-30', 40,
 ARRAY['Swimming Pool','Gym','Clubhouse','Cricket Ground','Badminton Court','Cycling Track','Senior Citizen Park','Temple'],
 280000000, 750000000, 1, 3, 550, 1400, 'ACTIVE', true,
 ARRAY['SBI','HDFC','IOB','Indian Bank','Canara']),

('9b563ce0-a97d-4cfa-a103-da0db783557e', 'Sobha Limited', 'Sobha Dream Acres', 'Where Dreams Come Home',
 'Premium plotted development near Whitefield. BMRDA approved plots with excellent appreciation potential. Club house and sports facilities included.',
 'PRM/KA/RERA/1251/309/AG/181126/001973', true, 'Bangalore', 'Karnataka', 'Whitefield', '560067', 12.9950, 77.7500,
 600, 250, 0, 0, 'READY_TO_MOVE', '2022-01-01', '2025-12-31', 100,
 ARRAY['Clubhouse','Swimming Pool','Tennis Court','Jogging Track','Organic Garden','Amphitheatre','Children Play Area'],
 3000000000, 12000000000, 0, 0, 1200, 5000, 'ACTIVE', true,
 ARRAY['SBI','HDFC','ICICI','Axis']),

('dfa2f343-4ea1-4a41-bb56-1c65553b47e6', 'Mahindra Lifespace', 'Mahindra Eden', 'Green Living Redefined',
 'Eco-friendly residential project in Vaishali Nagar, Jaipur. IGBC Gold certified. Surrounded by Aravalli foothills.',
 'RAJ/P/2024/001234', true, 'Jaipur', 'Rajasthan', 'Vaishali Nagar', '302021', 26.9154, 75.7229,
 500, 380, 5, 15, 'UPCOMING', '2025-09-01', '2029-03-31', 5,
 ARRAY['Swimming Pool','Gym','Clubhouse','Yoga Deck','Organic Farm','Butterfly Garden','Solar Panels','Rainwater Harvesting','EV Charging'],
 450000000, 1500000000, 2, 4, 800, 2200, 'ACTIVE', true,
 ARRAY['SBI','HDFC','BOB','PNB']);

-- ── Seed Unit Types for Builder Projects ─────────────────────────────────

INSERT INTO project_unit_types (project_id, name, bhk, built_up_area_sqft, carpet_area_sqft, base_price_paise, total_units, bathrooms, balconies, furnishing)
SELECT bp.id, ut.name, ut.bhk, ut.built_up_area_sqft, ut.carpet_area_sqft, ut.base_price_paise, ut.total_units, ut.bathrooms, ut.balconies, ut.furnishing
FROM builder_projects bp
CROSS JOIN (VALUES
  ('2 BHK Classic', 2, 1200, 950, 1250000000::bigint, 140, 2, 1, 'UNFURNISHED'),
  ('3 BHK Premium', 3, 1800, 1450, 2200000000::bigint, 180, 3, 2, 'SEMI_FURNISHED'),
  ('4 BHK Luxury', 4, 3500, 2800, 4500000000::bigint, 100, 4, 3, 'FURNISHED')
) AS ut(name, bhk, built_up_area_sqft, carpet_area_sqft, base_price_paise, total_units, bathrooms, balconies, furnishing)
WHERE bp.builder_name = 'Godrej Properties';
