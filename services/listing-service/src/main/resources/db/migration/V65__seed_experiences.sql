-- Seed dummy experiences (ACTIVE) for UI display
-- Using a fixed host_id placeholder — update if needed
DO $$
DECLARE
    host UUID := '00000000-0000-0000-0000-000000000001';
BEGIN

INSERT INTO listings.experiences (id, host_id, title, description, category, city, location_name, duration_hours, max_guests, price_paise, languages_spoken, media_urls, status, rating, review_count)
VALUES
-- CULINARY
('a0000000-0000-0000-0000-000000000001', host,
 'Old Delhi Street Food Walk', 'Explore the legendary lanes of Chandni Chowk tasting paranthas, jalebis, chole bhature, and more with a local foodie guide.',
 'CULINARY', 'Delhi', 'Chandni Chowk', 3.0, 12, 150000, 'en,hi', '', 'ACTIVE', 4.80, 47),

('a0000000-0000-0000-0000-000000000002', host,
 'Rajasthani Thali Cooking Class', 'Learn to make dal baati churma, gatte ki sabzi, and ker sangri in a heritage haveli kitchen.',
 'CULINARY', 'Jaipur', 'Nahargarh Haveli', 4.0, 8, 250000, 'en,hi', '', 'ACTIVE', 4.90, 32),

('a0000000-0000-0000-0000-000000000003', host,
 'Kerala Spice Garden & Lunch', 'Tour a working spice plantation, pick your own spices, and cook a traditional Sadya lunch.',
 'CULINARY', 'Munnar', 'Kanan Devan Hills', 5.0, 10, 200000, 'en,ml', '', 'ACTIVE', 4.70, 21),

-- CULTURAL
('a0000000-0000-0000-0000-000000000004', host,
 'Varanasi Sunrise Boat & Temple Walk', 'Witness the magical Ganga aarti at dawn from a wooden boat, then walk through ancient temples and silk weaver lanes.',
 'CULTURAL', 'Varanasi', 'Dashashwamedh Ghat', 4.0, 10, 180000, 'en,hi', '', 'ACTIVE', 4.95, 89),

('a0000000-0000-0000-0000-000000000005', host,
 'Jaipur Heritage Photography Tour', 'Capture the pink city at golden hour — Hawa Mahal, Jantar Mantar, and hidden stepwells with a pro photographer guide.',
 'CULTURAL', 'Jaipur', 'Hawa Mahal', 3.5, 8, 200000, 'en,hi', '', 'ACTIVE', 4.60, 28),

('a0000000-0000-0000-0000-000000000006', host,
 'Hampi Ruins Cycling Expedition', 'Cycle through UNESCO boulder landscapes, ancient temples, and royal enclosures with a history expert.',
 'CULTURAL', 'Hampi', 'Virupaksha Temple', 5.0, 12, 175000, 'en,kn', '', 'ACTIVE', 4.85, 36),

-- WELLNESS
('a0000000-0000-0000-0000-000000000007', host,
 'Rishikesh Yoga & Meditation Retreat', 'Start with sunrise yoga on the Ganga banks, followed by pranayama, meditation, and Ayurvedic tea.',
 'WELLNESS', 'Rishikesh', 'Ram Jhula', 3.0, 15, 120000, 'en,hi', '', 'ACTIVE', 4.75, 54),

('a0000000-0000-0000-0000-000000000008', host,
 'Goa Beach Sunrise Yoga', 'Practice vinyasa flow on a quiet South Goa beach as the sun rises over the Arabian Sea.',
 'WELLNESS', 'Goa', 'Palolem Beach', 1.5, 20, 80000, 'en', '', 'ACTIVE', 4.50, 19),

-- ADVENTURE
('a0000000-0000-0000-0000-000000000009', host,
 'Manali Paragliding & Mountain Trek', 'Soar above the Kullu Valley on a tandem paraglide, then trek through pine forests to a hidden waterfall.',
 'ADVENTURE', 'Manali', 'Solang Valley', 6.0, 6, 350000, 'en,hi', '', 'ACTIVE', 4.90, 42),

('a0000000-0000-0000-0000-000000000010', host,
 'Meghalaya Living Root Bridge Trek', 'Trek down to the famous double-decker living root bridges of Nongriat with a Khasi tribal guide.',
 'ADVENTURE', 'Cherrapunji', 'Nongriat Village', 8.0, 8, 280000, 'en,kh', '', 'ACTIVE', 4.95, 17),

('a0000000-0000-0000-0000-000000000011', host,
 'Udaipur Kayaking on Lake Pichola', 'Paddle past the City Palace and Jag Mandir at sunset on calm lake waters.',
 'ADVENTURE', 'Udaipur', 'Lake Pichola', 2.0, 10, 150000, 'en,hi', '', 'ACTIVE', 4.65, 23),

-- CREATIVE
('a0000000-0000-0000-0000-000000000012', host,
 'Jaipur Block Printing Workshop', 'Learn the 300-year-old art of hand block printing on fabric using natural dyes in a master artisan studio.',
 'CREATIVE', 'Jaipur', 'Sanganer', 3.0, 10, 180000, 'en,hi', '', 'ACTIVE', 4.80, 31),

('a0000000-0000-0000-0000-000000000013', host,
 'Kolkata Terracotta Pottery Session', 'Shape your own pottery on a traditional wheel guided by Kumartuli idol makers.',
 'CREATIVE', 'Kolkata', 'Kumartuli', 2.5, 8, 140000, 'en,bn', '', 'ACTIVE', 4.70, 14),

('a0000000-0000-0000-0000-000000000014', host,
 'Mumbai Bollywood Dance Class', 'Learn iconic Bollywood choreography in a professional studio with a film-industry choreographer.',
 'CREATIVE', 'Mumbai', 'Andheri West', 2.0, 20, 160000, 'en,hi', '', 'ACTIVE', 4.55, 45);

END $$;
