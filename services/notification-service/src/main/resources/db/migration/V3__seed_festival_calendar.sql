-- V3: Seed Indian festival calendar for 2026
-- Used by notification-service to trigger marketing campaigns around festivals

CREATE TABLE IF NOT EXISTS notifications.festival_calendar (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    festival_name       VARCHAR(100)  NOT NULL,
    festival_date       DATE          NOT NULL,
    region              VARCHAR(50),
    language_code       VARCHAR(10),
    campaign_subject    VARCHAR(255),
    campaign_headline   VARCHAR(255),
    campaign_body       TEXT,
    discovery_categories VARCHAR(500),
    target_cities       VARCHAR(500),
    is_active           BOOLEAN       NOT NULL DEFAULT true,
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_festival_calendar_date ON notifications.festival_calendar(festival_date);
CREATE INDEX idx_festival_calendar_region ON notifications.festival_calendar(region);
CREATE INDEX idx_festival_calendar_active ON notifications.festival_calendar(is_active);

-- ============================================================
-- NATIONAL / ALL-INDIA FESTIVALS
-- ============================================================

INSERT INTO notifications.festival_calendar
    (festival_name, festival_date, region, language_code, campaign_subject, campaign_headline, campaign_body, discovery_categories, target_cities)
VALUES
    -- 1. New Year
    ('New Year', '2026-01-01', NULL, NULL,
     'Ring in the New Year with Safar!',
     'Ring in the New Year',
     'Start 2026 on a high — beachside bonfires in Goa, rooftop parties in Mumbai, or a serene sunrise in the hills. Book your New Year escape before the best stays sell out!',
     'BEACH_VIBES,WEEKEND_GETAWAYS,HILL_STATIONS',
     'Goa,Mumbai,Manali,Rishikesh,Udaipur'),

    -- 2. Makar Sankranti
    ('Makar Sankranti', '2026-01-14', NULL, NULL,
     'Harvest season getaway — fly high this Sankranti!',
     'Harvest season getaway',
     'Kite-filled skies, til-gur sweetness, and golden fields. Celebrate the harvest season with a countryside retreat in Gujarat or a desert camp in Rajasthan.',
     'HERITAGE_HAVELIS,DESERT_GLAMPING,WEEKEND_GETAWAYS',
     'Ahmedabad,Jaipur,Jodhpur,Udaipur,Rann of Kutch'),

    -- 3. Republic Day
    ('Republic Day', '2026-01-26', NULL, NULL,
     'Celebrate the republic — explore incredible India!',
     'Celebrate the republic',
     'Honour the spirit of India with a long-weekend trip to the capital or a heritage stay that tells the story of the nation. Parade day vibes, patriotic pride, unforgettable stays.',
     'HERITAGE_HAVELIS,WEEKEND_GETAWAYS',
     'Delhi,Agra,Jaipur,Amritsar,Lucknow'),

    -- 4. Holi
    ('Holi', '2026-03-17', NULL, NULL,
     'Add colour to your travels this Holi!',
     'Add colour to your travels',
     'Splash into the festival of colours! Play Holi in the bylanes of Mathura, the palaces of Rajasthan, or the ghats of Varanasi. Heritage havelis and rooftop celebrations await.',
     'HERITAGE_HAVELIS,WEEKEND_GETAWAYS,DESERT_GLAMPING',
     'Mathura,Vrindavan,Jaipur,Udaipur,Varanasi,Pushkar'),

    -- 5. Eid al-Fitr
    ('Eid al-Fitr', '2026-03-30', NULL, NULL,
     'Eid Mubarak! Feast & rest with Safar',
     'Eid Mubarak! Feast & rest',
     'Celebrate the end of Ramadan with family in a spacious heritage home. Savour biryanis in Lucknow, haleem in Hyderabad, or kebabs in Old Delhi — your Eid feast deserves a grand setting.',
     'HERITAGE_HAVELIS,WEEKEND_GETAWAYS',
     'Lucknow,Hyderabad,Delhi,Bhopal,Kozhikode'),

    -- 6. Eid al-Adha
    ('Eid al-Adha', '2026-06-07', NULL, NULL,
     'Eid Mubarak! A family getaway awaits',
     'Eid Mubarak!',
     'Gather the family for Eid al-Adha in a heritage home with a full kitchen. From Lucknowi hospitality to Hyderabadi charm — celebrate togetherness in spaces that feel like home.',
     'HERITAGE_HAVELIS,WEEKEND_GETAWAYS',
     'Lucknow,Hyderabad,Delhi,Srinagar,Kozhikode'),

    -- 7. Independence Day
    ('Independence Day', '2026-08-15', NULL, NULL,
     'Explore incredible India this Independence Day!',
     'Explore incredible India',
     'Celebrate freedom with a journey through India''s heritage. Visit the forts of Rajasthan, the monuments of Delhi, or the battlefields of Punjab — every corner tells a story of pride.',
     'HERITAGE_HAVELIS,HILL_STATIONS,WEEKEND_GETAWAYS',
     'Delhi,Amritsar,Jaipur,Wagah,Ahmedabad,Goa'),

    -- 8. Diwali
    ('Diwali', '2026-10-20', NULL, NULL,
     'Light up your holiday this Diwali!',
     'Light up your holiday',
     'Diyas, fireworks, and fairy-lit havelis — spend Diwali away from the smoke in a hill-station retreat or a heritage palace. Early bookers get the brightest stays!',
     'HILL_STATIONS,HERITAGE_HAVELIS,WEEKEND_GETAWAYS',
     'Jaipur,Udaipur,Varanasi,Shimla,Manali,Mussoorie'),

    -- 9. Christmas
    ('Christmas', '2026-12-25', NULL, NULL,
     'A festive getaway for Christmas!',
     'A festive getaway',
     'Palm trees or pine trees — your call. Celebrate Christmas with a beach villa in Goa, a houseboat in Kerala, or a cosy cabin in the hills. Merry stays, merrier memories.',
     'BEACH_VIBES,HILL_STATIONS,BACKWATER_BLISS',
     'Goa,Kochi,Munnar,Pondicherry,Shimla,Manali');

-- ============================================================
-- REGIONAL FESTIVALS
-- ============================================================

INSERT INTO notifications.festival_calendar
    (festival_name, festival_date, region, language_code, campaign_subject, campaign_headline, campaign_body, discovery_categories, target_cities)
VALUES
    -- 10. Durga Puja (East / Bengali)
    ('Durga Puja', '2026-10-01', 'EAST', 'bn',
     'Celebrate Pujo with Safar!',
     'Celebrate Pujo!',
     'Dhunuchi naach, pandal hopping, and Kolkata''s legendary street food — experience Durga Puja like a true Bengali. Book a heritage stay near the best pandals and soak in the festivities.',
     'HERITAGE_HAVELIS,WEEKEND_GETAWAYS',
     'Kolkata,Howrah,Siliguri,Shantiniketan,Digha'),

    -- 11. Ganesh Chaturthi (West / Marathi)
    ('Ganesh Chaturthi', '2026-08-27', 'WEST', 'mr',
     'Ganapati Bappa Morya! Book your stay',
     'Ganapati Bappa Morya!',
     'Welcome Bappa home with a stay on the Konkan coast, a sea-facing flat in Mumbai, or a heritage wada in Pune. Modaks, mandals, and the magic of Ganpati await.',
     'BEACH_VIBES,HERITAGE_HAVELIS,WEEKEND_GETAWAYS',
     'Mumbai,Pune,Ratnagiri,Ganpatipule,Alibaug'),

    -- 12. Pongal (South / Tamil)
    ('Pongal', '2026-01-14', 'SOUTH', 'ta',
     'Happy Pongal! A village escape awaits',
     'Happy Pongal!',
     'Sugarcane, kolam, and the warmth of Tamil hospitality. Celebrate Pongal in a traditional village stay, a temple-town guesthouse, or a Chettinad mansion.',
     'HERITAGE_HAVELIS,WEEKEND_GETAWAYS',
     'Madurai,Thanjavur,Mahabalipuram,Pondicherry,Chettinad'),

    -- 13. Onam (South / Malayalam)
    ('Onam', '2026-09-05', 'SOUTH', 'ml',
     'Onam Ashamsakal! Kerala calling',
     'Onam Ashamsakal!',
     'Onasadya on a houseboat, Vallam Kali on the backwaters, and pookkalam at your doorstep. Celebrate Onam in God''s Own Country with a stay that feels like coming home.',
     'BACKWATER_BLISS,HILL_STATIONS,WEEKEND_GETAWAYS',
     'Alleppey,Kochi,Kumarakom,Munnar,Wayanad'),

    -- 14. Baisakhi (North / Punjabi)
    ('Baisakhi', '2026-04-13', 'NORTH', 'pa',
     'Happy Baisakhi! Punjab da swagat hai',
     'Happy Baisakhi!',
     'Bhangra, lassi, and golden wheat fields — celebrate the Punjabi New Year with a farm stay in Punjab or a heritage visit to the Golden Temple. Balle balle!',
     'HERITAGE_HAVELIS,WEEKEND_GETAWAYS',
     'Amritsar,Chandigarh,Ludhiana,Patiala,Jalandhar'),

    -- 15. Navratri / Dandiya (West / Gujarati)
    ('Navratri', '2026-10-11', 'WEST', 'gu',
     'Garba nights & getaways — Navratri with Safar!',
     'Garba nights & getaways',
     'Nine nights of dandiya, garba, and festive fervour. Book a stay near the best Navratri events in Gujarat or Rajasthan — dress up, dance, and make memories.',
     'HERITAGE_HAVELIS,DESERT_GLAMPING,WEEKEND_GETAWAYS',
     'Ahmedabad,Vadodara,Surat,Rajkot,Udaipur'),

    -- 16. Bihu (East / Assamese)
    ('Bihu', '2026-04-14', 'EAST', 'as',
     'Happy Bihu! Discover the Northeast',
     'Happy Bihu!',
     'Tea gardens, Bihu dances, and the mighty Brahmaputra — celebrate Rongali Bihu with a stay in Assam. From Kaziranga lodges to Majuli island retreats, the Northeast welcomes you.',
     'HILL_STATIONS,WEEKEND_GETAWAYS',
     'Guwahati,Jorhat,Kaziranga,Majuli,Shillong'),

    -- 17. Ugadi (South / Telugu)
    ('Ugadi', '2026-03-28', 'SOUTH', 'te',
     'Ugadi Subhakankshalu! New year, new journeys',
     'Ugadi Subhakankshalu!',
     'Welcome the Telugu New Year with a heritage stay in Andhra or Telangana. Ugadi pachadi, temple visits, and the warmth of Deccan hospitality — start the year right.',
     'HERITAGE_HAVELIS,WEEKEND_GETAWAYS',
     'Hyderabad,Vijayawada,Tirupati,Visakhapatnam,Warangal');

-- ============================================================
-- LONG WEEKENDS / SEASONAL
-- ============================================================

INSERT INTO notifications.festival_calendar
    (festival_name, festival_date, region, language_code, campaign_subject, campaign_headline, campaign_body, discovery_categories, target_cities)
VALUES
    -- 18. Summer Holidays
    ('Summer Holidays', '2026-05-01', NULL, NULL,
     'Summer escape plan sorted!',
     'Summer escape plan sorted',
     'Beat the heat with a hill-station hideaway or a coastal breeze. Whether it''s family time in Manali, surfing lessons in Goa, or lazy days in Coorg — your summer escape is one tap away.',
     'HILL_STATIONS,BEACH_VIBES,WEEKEND_GETAWAYS',
     'Manali,Shimla,Ooty,Coorg,Goa,Munnar,Darjeeling'),

    -- 19. Monsoon Retreat
    ('Monsoon Retreat', '2026-07-01', NULL, NULL,
     'Chase the rains with Safar!',
     'Chase the rains',
     'Mist-wrapped hills, roaring waterfalls, and the smell of wet earth. The monsoon is made for slow travel — book a Western Ghats cottage, a Coorg plantation stay, or a Munnar tea-estate retreat.',
     'HILL_STATIONS,BACKWATER_BLISS,WEEKEND_GETAWAYS',
     'Coorg,Munnar,Lonavala,Mahabaleshwar,Wayanad,Chikmagalur'),

    -- 20. Winter Break
    ('Winter Break', '2026-12-20', NULL, NULL,
     'Winter wonderland awaits!',
     'Winter wonderland',
     'Snow-capped peaks, bonfire nights, and hot chocolate mornings. Escape to Manali, Shimla, or Gulmarg for a picture-perfect winter break. The mountains are calling — book before the snow melts!',
     'HILL_STATIONS,WEEKEND_GETAWAYS',
     'Manali,Shimla,Gulmarg,Auli,Mussoorie,Dharamshala');
