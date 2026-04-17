-- ============================================================
-- Sale Property Expansion: Land, Agriculture, Legal, Agent
-- ============================================================

-- Land/Plot fields
ALTER TABLE listings.sale_properties ADD COLUMN total_acres DECIMAL(10,2);
ALTER TABLE listings.sale_properties ADD COLUMN plot_length_ft DECIMAL(10,2);
ALTER TABLE listings.sale_properties ADD COLUMN plot_breadth_ft DECIMAL(10,2);
ALTER TABLE listings.sale_properties ADD COLUMN boundary_wall BOOLEAN DEFAULT false;
ALTER TABLE listings.sale_properties ADD COLUMN corner_plot BOOLEAN DEFAULT false;
ALTER TABLE listings.sale_properties ADD COLUMN road_width_ft DECIMAL(10,2);
ALTER TABLE listings.sale_properties ADD COLUMN road_access VARCHAR(30);
ALTER TABLE listings.sale_properties ADD COLUMN zone_type VARCHAR(30);

-- Agriculture fields
ALTER TABLE listings.sale_properties ADD COLUMN irrigation_type VARCHAR(30);
ALTER TABLE listings.sale_properties ADD COLUMN soil_type VARCHAR(30);
ALTER TABLE listings.sale_properties ADD COLUMN water_source VARCHAR(30);
ALTER TABLE listings.sale_properties ADD COLUMN borewell_count INTEGER DEFAULT 0;
ALTER TABLE listings.sale_properties ADD COLUMN fencing_type VARCHAR(30);
ALTER TABLE listings.sale_properties ADD COLUMN organic_certified BOOLEAN DEFAULT false;
ALTER TABLE listings.sale_properties ADD COLUMN current_crop VARCHAR(100);

-- Legal fields
ALTER TABLE listings.sale_properties ADD COLUMN ownership_type VARCHAR(30);
ALTER TABLE listings.sale_properties ADD COLUMN title_clear BOOLEAN;
ALTER TABLE listings.sale_properties ADD COLUMN encumbrance_free BOOLEAN;
ALTER TABLE listings.sale_properties ADD COLUMN rera_number VARCHAR(50);
ALTER TABLE listings.sale_properties ADD COLUMN govt_approved BOOLEAN DEFAULT false;

-- Enhanced media (brochure_url already exists)
ALTER TABLE listings.sale_properties ADD COLUMN virtual_tour_url TEXT;
ALTER TABLE listings.sale_properties ADD COLUMN floor_plan_urls TEXT;

-- Agent
ALTER TABLE listings.sale_properties ADD COLUMN agent_id UUID;
ALTER TABLE listings.sale_properties ADD COLUMN agent_name VARCHAR(100);
ALTER TABLE listings.sale_properties ADD COLUMN agent_phone VARCHAR(20);
