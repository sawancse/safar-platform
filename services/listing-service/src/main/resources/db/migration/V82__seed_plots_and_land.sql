-- Seed 12 plot/land listings across major Indian cities for Plots & Land tab

INSERT INTO sale_properties (seller_id, seller_type, title, description, sale_property_type, transaction_type,
  locality, city, state, pincode, lat, lng, asking_price_paise, price_per_sqft_paise, price_negotiable,
  carpet_area_sqft, built_up_area_sqft, bedrooms, bathrooms, balconies, floor_number, total_floors,
  facing, property_age_years, furnishing, parking_covered, parking_open, possession_status,
  amenities, water_supply, power_backup, gated_community, status,
  plot_area_sqft, total_acres, road_access, zone_type, boundary_wall, title_clear)
VALUES
-- Hyderabad plots
('9b563ce0-a97d-4cfa-a103-da0db783557e', 'OWNER', 'HMDA Approved Plot in Shamshabad', 'HMDA approved residential plot near Rajiv Gandhi International Airport. Clear title, ready for construction. Gated layout with internal roads.', 'RESIDENTIAL_PLOT', 'NEW',
 'Shamshabad', 'Hyderabad', 'Telangana', '501218', 17.2403, 78.4294, 450000000, 300000, true,
 null, null, null, null, null, null, null, 'SOUTH', null, null, 0, 0, 'READY_TO_MOVE',
 ARRAY[]::text[], null, null, true, 'ACTIVE',
 1500, null, 'MAIN_ROAD', 'RESIDENTIAL', true, true),

('9b563ce0-a97d-4cfa-a103-da0db783557e', 'AGENT', 'Corner Plot Kompally', 'East-facing corner plot in Kompally near ORR. 30ft wide road, all utilities available. Approved layout.', 'PLOT', 'RESALE',
 'Kompally', 'Hyderabad', 'Telangana', '500014', 17.5340, 78.4840, 380000000, 280000, true,
 null, null, null, null, null, null, null, 'EAST', null, null, 0, 0, 'READY_TO_MOVE',
 ARRAY[]::text[], 'CORPORATION', null, false, 'ACTIVE',
 1357, null, 'INTERNAL', 'RESIDENTIAL', false, true),

-- Bangalore plots
('9b563ce0-a97d-4cfa-a103-da0db783557e', 'OWNER', 'BDA Approved Site Devanahalli', 'Premium residential site near Bangalore Airport. BDA approved, clear title, surrounded by reputed builder projects.', 'RESIDENTIAL_PLOT', 'NEW',
 'Devanahalli', 'Bangalore', 'Karnataka', '562110', 13.2462, 77.7125, 520000000, 350000, false,
 null, null, null, null, null, null, null, 'NORTH', null, null, 0, 0, 'READY_TO_MOVE',
 ARRAY[]::text[], 'BOREWELL', null, true, 'ACTIVE',
 1486, null, 'MAIN_ROAD', 'RESIDENTIAL', true, true),

('9b563ce0-a97d-4cfa-a103-da0db783557e', 'BUILDER', 'Commercial Plot Whitefield', 'Prime commercial plot in Whitefield IT Hub. Ideal for office building, retail, or mixed-use development. BBMP approved.', 'COMMERCIAL_LAND', 'NEW',
 'Whitefield', 'Bangalore', 'Karnataka', '560066', 12.9698, 77.7500, 2500000000, 1200000, true,
 null, null, null, null, null, null, null, 'WEST', null, null, 0, 0, 'READY_TO_MOVE',
 ARRAY[]::text[], 'CORPORATION', 'FULL', false, 'ACTIVE',
 2083, null, 'MAIN_ROAD', 'COMMERCIAL', true, true),

-- Mumbai plots
('9b563ce0-a97d-4cfa-a103-da0db783557e', 'AGENT', 'Industrial Land Bhiwandi', 'Industrial zone land near Bhiwandi logistics hub. 40ft wide approach road, electricity and water available. Ideal for warehouse.', 'INDUSTRIAL_LAND', 'NEW',
 'Bhiwandi', 'Mumbai', 'Maharashtra', '421302', 19.2813, 73.0483, 3200000000, 800000, true,
 null, null, null, null, null, null, null, null, null, null, 0, 0, 'READY_TO_MOVE',
 ARRAY[]::text[], 'CORPORATION', 'FULL', false, 'ACTIVE',
 4000, 0.09, 'MAIN_ROAD', 'INDUSTRIAL', true, true),

('9b563ce0-a97d-4cfa-a103-da0db783557e', 'OWNER', 'NA Plot Panvel', 'Non-agricultural residential plot in upcoming Navi Mumbai zone. CIDCO approved. Near proposed metro station.', 'PLOT', 'NEW',
 'Panvel', 'Mumbai', 'Maharashtra', '410206', 18.9894, 73.1175, 680000000, 450000, true,
 null, null, null, null, null, null, null, 'EAST', null, null, 0, 0, 'READY_TO_MOVE',
 ARRAY[]::text[], 'CORPORATION', null, true, 'ACTIVE',
 1511, null, 'INTERNAL', 'RESIDENTIAL', false, true),

-- Delhi NCR plots
('9b563ce0-a97d-4cfa-a103-da0db783557e', 'BUILDER', 'Gated Plot Sector 150 Noida', 'Premium gated community plot in Sector 150 Noida. Swimming pool, clubhouse, 24/7 security. DDA approved.', 'RESIDENTIAL_PLOT', 'NEW',
 'Sector 150', 'Noida', 'Uttar Pradesh', '201310', 28.4581, 77.5192, 750000000, 500000, false,
 null, null, null, null, null, null, null, 'NORTH', null, null, 0, 0, 'READY_TO_MOVE',
 ARRAY['Swimming Pool','Clubhouse','CCTV','Power Backup'], null, 'FULL', true, 'ACTIVE',
 1500, null, 'INTERNAL', 'RESIDENTIAL', true, true),

('9b563ce0-a97d-4cfa-a103-da0db783557e', 'AGENT', 'Commercial Plot Gurgaon', 'SCO (Shop-cum-Office) plot in Sector 89 Gurgaon. On main 60ft road, walking distance from HUDA metro.', 'COMMERCIAL_LAND', 'RESALE',
 'Sector 89', 'Gurgaon', 'Haryana', '122505', 28.4107, 76.9749, 1800000000, 900000, true,
 null, null, null, null, null, null, null, 'SOUTH', null, null, 0, 0, 'READY_TO_MOVE',
 ARRAY[]::text[], null, null, false, 'ACTIVE',
 2000, null, 'MAIN_ROAD', 'COMMERCIAL', false, true),

-- Chennai plots
('9b563ce0-a97d-4cfa-a103-da0db783557e', 'OWNER', 'DTCP Approved Plot ECR', 'DTCP approved residential plot on East Coast Road. Beach facing, perfect for weekend villa. Clear patta.', 'PLOT', 'RESALE',
 'ECR', 'Chennai', 'Tamil Nadu', '603104', 12.8303, 80.2521, 420000000, 320000, true,
 null, null, null, null, null, null, null, 'EAST', null, null, 0, 0, 'READY_TO_MOVE',
 ARRAY[]::text[], 'BOREWELL', null, false, 'ACTIVE',
 1312, null, 'MAIN_ROAD', 'RESIDENTIAL', false, true),

-- Pune plots
('9b563ce0-a97d-4cfa-a103-da0db783557e', 'OWNER', 'NA Plot Hinjewadi Phase 3', 'Non-agricultural plot near Hinjewadi IT Park Phase 3. Ideal for residential construction. All utilities available.', 'RESIDENTIAL_PLOT', 'NEW',
 'Hinjewadi', 'Pune', 'Maharashtra', '411057', 18.5912, 73.7390, 350000000, 280000, true,
 null, null, null, null, null, null, null, 'WEST', null, null, 0, 0, 'READY_TO_MOVE',
 ARRAY[]::text[], 'BOREWELL', null, false, 'ACTIVE',
 1250, null, 'INTERNAL', 'RESIDENTIAL', false, true),

-- Ahmedabad (additional plot)
('9b563ce0-a97d-4cfa-a103-da0db783557e', 'BUILDER', 'Gated Plots South Bopal', 'Premium gated township plots in South Bopal. All amenities, underground electricity, wide internal roads.', 'RESIDENTIAL_PLOT', 'NEW',
 'South Bopal', 'Ahmedabad', 'Gujarat', '380058', 23.0100, 72.4900, 480000000, 350000, false,
 null, null, null, null, null, null, null, 'NORTH', null, null, 0, 0, 'READY_TO_MOVE',
 ARRAY['Clubhouse','Garden','CCTV','Power Backup'], 'CORPORATION', 'FULL', true, 'ACTIVE',
 1371, null, 'INTERNAL', 'RESIDENTIAL', true, true),

-- Kolkata
('9b563ce0-a97d-4cfa-a103-da0db783557e', 'AGENT', 'Plot New Town Rajarhat', 'HIDCO approved plot in Action Area III, New Town Rajarhat. Near Eco Park and IT Hub. Clear mutation.', 'PLOT', 'RESALE',
 'New Town', 'Kolkata', 'West Bengal', '700156', 22.5958, 88.4833, 280000000, 220000, true,
 null, null, null, null, null, null, null, 'SOUTH', null, null, 0, 0, 'READY_TO_MOVE',
 ARRAY[]::text[], 'CORPORATION', null, false, 'ACTIVE',
 1272, null, 'MAIN_ROAD', 'RESIDENTIAL', false, true);
