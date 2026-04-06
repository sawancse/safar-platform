-- V69: Fix enum values in seed data
UPDATE listings.sale_properties SET furnishing = 'FURNISHED' WHERE furnishing = 'FULLY_FURNISHED';
UPDATE listings.sale_properties SET transaction_type = 'NEW_BOOKING' WHERE transaction_type = 'NEW';
UPDATE listings.project_unit_types SET furnishing = 'FURNISHED' WHERE furnishing = 'Fully Furnished';
UPDATE listings.project_unit_types SET furnishing = 'UNFURNISHED' WHERE furnishing = 'Unfurnished';
UPDATE listings.project_unit_types SET furnishing = 'SEMI_FURNISHED' WHERE furnishing = 'Semi-Furnished';
