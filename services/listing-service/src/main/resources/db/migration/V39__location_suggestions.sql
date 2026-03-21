-- Location Suggestions table for autocomplete
CREATE TABLE IF NOT EXISTS listings.location_suggestions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(200) NOT NULL,
    display_name VARCHAR(300) NOT NULL,
    type VARCHAR(30) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100),
    lat NUMERIC(9,6) NOT NULL,
    lng NUMERIC(9,6) NOT NULL,
    popularity_score INTEGER DEFAULT 0,
    default_radius_km DOUBLE PRECISION DEFAULT 5.0
);

CREATE INDEX IF NOT EXISTS idx_location_name ON listings.location_suggestions(name);
CREATE INDEX IF NOT EXISTS idx_location_city ON listings.location_suggestions(city);
CREATE INDEX IF NOT EXISTS idx_location_type ON listings.location_suggestions(type);

-- Seed data: Major Indian cities
INSERT INTO listings.location_suggestions (name, display_name, type, city, state, lat, lng, popularity_score, default_radius_km) VALUES
('Hyderabad', 'Hyderabad, Telangana', 'CITY', 'Hyderabad', 'Telangana', 17.385044, 78.486671, 100, 15.0),
('Bangalore', 'Bangalore, Karnataka', 'CITY', 'Bangalore', 'Karnataka', 12.971599, 77.594566, 100, 15.0),
('Mumbai', 'Mumbai, Maharashtra', 'CITY', 'Mumbai', 'Maharashtra', 19.076090, 72.877426, 100, 20.0),
('Delhi', 'New Delhi', 'CITY', 'Delhi', 'Delhi', 28.613939, 77.209021, 100, 20.0),
('Chennai', 'Chennai, Tamil Nadu', 'CITY', 'Chennai', 'Tamil Nadu', 13.082680, 80.270718, 100, 15.0),
('Pune', 'Pune, Maharashtra', 'CITY', 'Pune', 'Maharashtra', 18.520430, 73.856743, 90, 15.0),
('Kolkata', 'Kolkata, West Bengal', 'CITY', 'Kolkata', 'West Bengal', 22.572646, 88.363895, 90, 15.0),
('Goa', 'Goa', 'CITY', 'Goa', 'Goa', 15.299327, 74.123996, 85, 20.0),
('Jaipur', 'Jaipur, Rajasthan', 'CITY', 'Jaipur', 'Rajasthan', 26.912434, 75.787270, 80, 15.0),
('Ahmedabad', 'Ahmedabad, Gujarat', 'CITY', 'Ahmedabad', 'Gujarat', 23.022505, 72.571365, 80, 15.0),
-- Hyderabad localities
('Madhapur', 'Madhapur, Hyderabad', 'LOCALITY', 'Hyderabad', 'Telangana', 17.448294, 78.391487, 80, 3.0),
('Gachibowli', 'Gachibowli, Hyderabad', 'LOCALITY', 'Hyderabad', 'Telangana', 17.440081, 78.348915, 75, 3.0),
('Kondapur', 'Kondapur, Hyderabad', 'LOCALITY', 'Hyderabad', 'Telangana', 17.459849, 78.363770, 70, 3.0),
('Banjara Hills', 'Banjara Hills, Hyderabad', 'LOCALITY', 'Hyderabad', 'Telangana', 17.415877, 78.440788, 70, 2.0),
('Jubilee Hills', 'Jubilee Hills, Hyderabad', 'LOCALITY', 'Hyderabad', 'Telangana', 17.432488, 78.407684, 65, 2.0),
('Kukatpally', 'Kukatpally, Hyderabad', 'LOCALITY', 'Hyderabad', 'Telangana', 17.494646, 78.399086, 60, 3.0),
('Ameerpet', 'Ameerpet, Hyderabad', 'LOCALITY', 'Hyderabad', 'Telangana', 17.437464, 78.448563, 55, 2.0),
-- Hyderabad IT Parks
('HITEC City', 'HITEC City, Hyderabad', 'IT_PARK', 'Hyderabad', 'Telangana', 17.445710, 78.380468, 90, 3.0),
('Raheja Mindspace', 'Raheja Mindspace, Hyderabad', 'IT_PARK', 'Hyderabad', 'Telangana', 17.448793, 78.381210, 70, 2.0),
-- Hyderabad Colleges
('IIIT Hyderabad', 'IIIT Hyderabad, Gachibowli', 'COLLEGE', 'Hyderabad', 'Telangana', 17.445363, 78.349547, 60, 3.0),
('ISB Hyderabad', 'ISB, Gachibowli, Hyderabad', 'COLLEGE', 'Hyderabad', 'Telangana', 17.425102, 78.338371, 55, 3.0),
('Osmania University', 'Osmania University, Hyderabad', 'COLLEGE', 'Hyderabad', 'Telangana', 17.412929, 78.527481, 50, 3.0),
-- Hyderabad Hospitals
('Apollo Jubilee Hills', 'Apollo Hospital, Jubilee Hills, Hyderabad', 'HOSPITAL', 'Hyderabad', 'Telangana', 17.427155, 78.408393, 65, 3.0),
('KIMS Secunderabad', 'KIMS Hospital, Secunderabad', 'HOSPITAL', 'Hyderabad', 'Telangana', 17.437897, 78.498779, 50, 3.0),
-- Hyderabad Transit
('Secunderabad Railway Station', 'Secunderabad Railway Station', 'TRANSIT', 'Hyderabad', 'Telangana', 17.433753, 78.501040, 60, 3.0),
('Hyderabad Airport', 'Rajiv Gandhi Intl Airport, Shamshabad', 'TRANSIT', 'Hyderabad', 'Telangana', 17.240263, 78.429385, 70, 5.0),
-- Bangalore localities
('Koramangala', 'Koramangala, Bangalore', 'LOCALITY', 'Bangalore', 'Karnataka', 12.935202, 77.624432, 80, 3.0),
('Whitefield', 'Whitefield, Bangalore', 'LOCALITY', 'Bangalore', 'Karnataka', 12.969854, 77.750500, 75, 3.0),
('Indiranagar', 'Indiranagar, Bangalore', 'LOCALITY', 'Bangalore', 'Karnataka', 12.978103, 77.640530, 70, 2.0),
('HSR Layout', 'HSR Layout, Bangalore', 'LOCALITY', 'Bangalore', 'Karnataka', 12.912287, 77.635177, 65, 3.0),
('Electronic City', 'Electronic City, Bangalore', 'IT_PARK', 'Bangalore', 'Karnataka', 12.839660, 77.677620, 75, 5.0),
('Manyata Tech Park', 'Manyata Tech Park, Bangalore', 'IT_PARK', 'Bangalore', 'Karnataka', 13.046589, 77.621574, 65, 3.0)
ON CONFLICT DO NOTHING;
