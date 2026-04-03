-- Allow inquiries for builder projects (not just sale properties)
ALTER TABLE listings.property_inquiries ALTER COLUMN sale_property_id DROP NOT NULL;
ALTER TABLE listings.property_inquiries ADD COLUMN IF NOT EXISTS builder_project_id UUID;
CREATE INDEX IF NOT EXISTS idx_inquiry_builder_project ON listings.property_inquiries(builder_project_id);
