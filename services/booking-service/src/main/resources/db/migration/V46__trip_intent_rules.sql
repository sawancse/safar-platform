-- Trip Intent rule table — drives the cross-vertical suggestion engine
-- per Tree 5 of docs/tbo-integration-design.md.
--
-- Each rule specifies a trigger (route / destination / date / group) that,
-- when matched against a Trip context, infers the trip's purpose and
-- recommends a set of verticals (stay / cab / cook / pandit / etc.) the
-- user might also want to book.
--
-- Day-1 the engine runs rule-based; ML Trip-DNA ships in Phase 3 and will
-- replace this engine while keeping the same input/output contract.

CREATE TABLE IF NOT EXISTS bookings.trip_intent_rules (
    id                  UUID         PRIMARY KEY,
    rule_name           VARCHAR(120) NOT NULL,
    -- Lower = higher priority. 10=PILGRIMAGE, 20=DATE-CONTEXT, 30=ROUTE,
    -- 35=MEDICAL, 40=GROUP, 99=FALLBACK.
    priority            INT          NOT NULL,
    trigger_type        VARCHAR(20)  NOT NULL,             -- DESTINATION / ROUTE / DATE / GROUP / HISTORY / COMPOUND / FALLBACK
    trigger_value       JSONB        NOT NULL,             -- e.g. {"cities":["TIR","IXM"]} or {"month":11,"day_range":[10,16]}
    inferred_intent     VARCHAR(30)  NOT NULL,             -- TripIntent enum value
    suggested_verticals TEXT[]       NOT NULL,             -- ["STAY","COOK","PANDIT","CAB"]
    vertical_filters    JSONB,                             -- e.g. {"COOK":{"diet":"sattvik"}, "STAY":{"vegetarian_only":true}}
    applies_to_country  TEXT[]       NOT NULL DEFAULT ARRAY['IN']::TEXT[],
    enabled             BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by_user_id  UUID                                 -- for ops audit trail
);

CREATE INDEX IF NOT EXISTS idx_trip_intent_rules_enabled_priority
    ON bookings.trip_intent_rules (enabled, priority);
CREATE INDEX IF NOT EXISTS idx_trip_intent_rules_trigger_type
    ON bookings.trip_intent_rules (trigger_type);

-- ===================================================================
-- SEED — 40 rules covering the major Indian travel patterns Day-1
-- ===================================================================

-- Priority 10 — PILGRIMAGE (highest priority; salient across festivals/seasons)
INSERT INTO bookings.trip_intent_rules (id, rule_name, priority, trigger_type, trigger_value, inferred_intent, suggested_verticals, vertical_filters)
VALUES
  (gen_random_uuid(), 'Tirupati pilgrimage', 10, 'DESTINATION', '{"cities":["TIR"]}', 'PILGRIMAGE',
   ARRAY['STAY','COOK','PANDIT','CAB'], '{"COOK":{"diet":"sattvik"}, "STAY":{"vegetarian_only":true}}'),
  (gen_random_uuid(), 'Madurai/Rameshwaram pilgrimage', 10, 'DESTINATION', '{"cities":["IXM"]}', 'PILGRIMAGE',
   ARRAY['STAY','COOK','PANDIT','CAB'], '{"COOK":{"diet":"sattvik"}, "STAY":{"vegetarian_only":true}}'),
  (gen_random_uuid(), 'Vaishno Devi pilgrimage', 10, 'DESTINATION', '{"cities":["IXJ","SXR"]}', 'PILGRIMAGE',
   ARRAY['STAY','COOK','PANDIT','CAB'], '{"COOK":{"diet":"sattvik"}, "STAY":{"vegetarian_only":true}}'),
  (gen_random_uuid(), 'Varanasi pilgrimage', 10, 'DESTINATION', '{"cities":["VNS"]}', 'PILGRIMAGE',
   ARRAY['STAY','COOK','PANDIT','CAB'], '{"COOK":{"diet":"sattvik"}, "STAY":{"vegetarian_only":true}}'),
  (gen_random_uuid(), 'Sabarimala pilgrimage', 10, 'DESTINATION', '{"cities":["TRV"]}', 'PILGRIMAGE',
   ARRAY['STAY','COOK','PANDIT','CAB'], '{"COOK":{"diet":"sattvik"}, "STAY":{"vegetarian_only":true}}'),
  (gen_random_uuid(), 'Shirdi pilgrimage', 10, 'DESTINATION', '{"cities":["SAG"]}', 'PILGRIMAGE',
   ARRAY['STAY','COOK','PANDIT','CAB'], '{"COOK":{"diet":"sattvik"}, "STAY":{"vegetarian_only":true}}'),
  (gen_random_uuid(), 'Haridwar/Rishikesh pilgrimage', 10, 'DESTINATION', '{"cities":["DED"]}', 'PILGRIMAGE',
   ARRAY['STAY','COOK','PANDIT','CAB'], '{"COOK":{"diet":"sattvik"}, "STAY":{"vegetarian_only":true}}'),
  (gen_random_uuid(), 'Pushkar pilgrimage', 10, 'DESTINATION', '{"cities":["AJL","JAI"]}', 'PILGRIMAGE',
   ARRAY['STAY','COOK','PANDIT','CAB'], '{"COOK":{"diet":"sattvik"}, "STAY":{"vegetarian_only":true}}'),
  (gen_random_uuid(), 'Khatu Shyam pilgrimage', 10, 'DESTINATION', '{"cities":["JAI"]}', 'PILGRIMAGE',
   ARRAY['STAY','COOK','PANDIT','CAB'], '{"COOK":{"diet":"sattvik"}, "STAY":{"vegetarian_only":true}}'),
  (gen_random_uuid(), 'Ujjain Mahakal pilgrimage', 10, 'DESTINATION', '{"cities":["IDR"]}', 'PILGRIMAGE',
   ARRAY['STAY','COOK','PANDIT','CAB'], '{"COOK":{"diet":"sattvik"}, "STAY":{"vegetarian_only":true}}');

-- Priority 20 — DATE-CONTEXT (festivals + wedding season override route/group rules)
INSERT INTO bookings.trip_intent_rules (id, rule_name, priority, trigger_type, trigger_value, inferred_intent, suggested_verticals, vertical_filters)
VALUES
  (gen_random_uuid(), 'Diwali home trip', 20, 'DATE',
   '{"festival":"diwali","month":11,"day_range":[10,16],"to_home_city":true}', 'FAMILY',
   ARRAY['COOK','CAB','EXPERIENCE'], '{"COOK":{"menu":"festival"}}'),
  (gen_random_uuid(), 'Christmas/New Year leisure', 20, 'DATE',
   '{"month":12,"day_range":[20,31],"destination_type":"leisure"}', 'LEISURE',
   ARRAY['STAY','SPA','EXPERIENCE','CAB'], '{"STAY":{"tier":"premium"}}'),
  (gen_random_uuid(), 'New Year week leisure', 20, 'DATE',
   '{"month":1,"day_range":[1,5],"destination_type":"leisure"}', 'LEISURE',
   ARRAY['STAY','SPA','EXPERIENCE','CAB'], '{"STAY":{"tier":"premium"}}'),
  (gen_random_uuid(), 'Holi home trip', 20, 'DATE',
   '{"festival":"holi","month":3,"day_range":[8,16],"to_home_city":true}', 'FAMILY',
   ARRAY['COOK','CAB'], '{"COOK":{"menu":"festival"}}'),
  (gen_random_uuid(), 'Eid home trip', 20, 'DATE',
   '{"festival":"eid","to_home_city":true}', 'FAMILY',
   ARRAY['COOK','CAB'], '{"COOK":{"menu":"festival"}}'),
  (gen_random_uuid(), 'Wedding-season group bundle', 20, 'COMPOUND',
   '{"months":[11,12,1,2],"min_pax":4,"non_pilgrimage":true}', 'WEDDING',
   ARRAY['STAY','COOK','DECOR','PANDIT','CAB'], '{"STAY":{"type":"apartment"}, "COOK":{"menu":"banquet"}}'),
  (gen_random_uuid(), 'Summer vacation family', 20, 'DATE',
   '{"month":[5,6],"min_pax":3,"destination_type":"leisure"}', 'LEISURE',
   ARRAY['STAY','EXPERIENCE','CAB'], '{}'),
  (gen_random_uuid(), 'Monsoon getaway', 20, 'DATE',
   '{"month":[7,8],"destination_type":"hill"}', 'LEISURE',
   ARRAY['STAY','EXPERIENCE','CAB'], '{}');

-- Priority 30 — ROUTE / CORRIDOR (IT corridor, weekend metros, home-town routes)
INSERT INTO bookings.trip_intent_rules (id, rule_name, priority, trigger_type, trigger_value, inferred_intent, suggested_verticals, vertical_filters)
VALUES
  (gen_random_uuid(), 'IT corridor — BLR-HYD', 30, 'ROUTE',
   '{"routes":[["BLR","HYD"],["HYD","BLR"]],"max_duration_days":2,"max_pax":1}', 'BUSINESS',
   ARRAY['STAY','CAB','INSURANCE'], '{"STAY":{"type":"hotel","near":"airport"}}'),
  (gen_random_uuid(), 'IT corridor — BLR-PUN', 30, 'ROUTE',
   '{"routes":[["BLR","PNQ"],["PNQ","BLR"]],"max_duration_days":2,"max_pax":1}', 'BUSINESS',
   ARRAY['STAY','CAB','INSURANCE'], '{"STAY":{"type":"hotel","near":"airport"}}'),
  (gen_random_uuid(), 'IT corridor — BLR-CHN', 30, 'ROUTE',
   '{"routes":[["BLR","MAA"],["MAA","BLR"]],"max_duration_days":2,"max_pax":1}', 'BUSINESS',
   ARRAY['STAY','CAB','INSURANCE'], '{"STAY":{"type":"hotel","near":"airport"}}'),
  (gen_random_uuid(), 'IT corridor — BLR-BOM', 30, 'ROUTE',
   '{"routes":[["BLR","BOM"],["BOM","BLR"]],"max_duration_days":2,"max_pax":1}', 'BUSINESS',
   ARRAY['STAY','CAB','INSURANCE'], '{"STAY":{"type":"hotel","near":"airport"}}'),
  (gen_random_uuid(), 'IT corridor — HYD-PUN', 30, 'ROUTE',
   '{"routes":[["HYD","PNQ"],["PNQ","HYD"]],"max_duration_days":2,"max_pax":1}', 'BUSINESS',
   ARRAY['STAY','CAB','INSURANCE'], '{"STAY":{"type":"hotel","near":"airport"}}'),
  (gen_random_uuid(), 'IT corridor — HYD-BOM', 30, 'ROUTE',
   '{"routes":[["HYD","BOM"],["BOM","HYD"]],"max_duration_days":2,"max_pax":1}', 'BUSINESS',
   ARRAY['STAY','CAB','INSURANCE'], '{"STAY":{"type":"hotel","near":"airport"}}'),
  (gen_random_uuid(), 'Outbound to home city — long stay', 30, 'COMPOUND',
   '{"to_home_city":true,"min_duration_days":4}', 'FAMILY',
   ARRAY['CAB'], '{}'),
  (gen_random_uuid(), 'Weekend metro getaway', 30, 'COMPOUND',
   '{"departure_dow":[5],"return_dow":[7],"destination_type":"metro"}', 'LEISURE',
   ARRAY['STAY','CAB','EXPERIENCE'], '{}');

-- Priority 35 — MEDICAL TRAVEL
INSERT INTO bookings.trip_intent_rules (id, rule_name, priority, trigger_type, trigger_value, inferred_intent, suggested_verticals, vertical_filters)
VALUES
  (gen_random_uuid(), 'Apollo Chennai medical', 35, 'COMPOUND',
   '{"destination":"MAA","user_flag":"medical_history"}', 'MEDICAL',
   ARRAY['STAY','COOK','CAB'], '{"STAY":{"type":"apartment","near":"hospital_apollo_chennai"}, "COOK":{"diet":"medical"}}'),
  (gen_random_uuid(), 'Manipal Bangalore medical', 35, 'COMPOUND',
   '{"destination":"BLR","user_flag":"medical_history"}', 'MEDICAL',
   ARRAY['STAY','COOK','CAB'], '{"STAY":{"type":"apartment","near":"hospital_manipal_bangalore"}, "COOK":{"diet":"medical"}}'),
  (gen_random_uuid(), 'Hinduja Mumbai medical', 35, 'COMPOUND',
   '{"destination":"BOM","user_flag":"medical_history"}', 'MEDICAL',
   ARRAY['STAY','COOK','CAB'], '{"STAY":{"type":"apartment","near":"hospital_hinduja_mumbai"}, "COOK":{"diet":"medical"}}'),
  (gen_random_uuid(), 'AIIMS Delhi medical', 35, 'COMPOUND',
   '{"destination":"DEL","user_flag":"medical_history"}', 'MEDICAL',
   ARRAY['STAY','COOK','CAB'], '{"STAY":{"type":"apartment","near":"hospital_aiims_delhi"}, "COOK":{"diet":"medical"}}'),
  (gen_random_uuid(), 'Apollo Hyderabad medical', 35, 'COMPOUND',
   '{"destination":"HYD","user_flag":"medical_history"}', 'MEDICAL',
   ARRAY['STAY','COOK','CAB'], '{"STAY":{"type":"apartment","near":"hospital_apollo_hyderabad"}, "COOK":{"diet":"medical"}}'),
  (gen_random_uuid(), 'CMC Vellore medical', 35, 'COMPOUND',
   '{"destination":"MAA","user_flag":"medical_history","sub_locality":"vellore"}', 'MEDICAL',
   ARRAY['STAY','COOK','CAB'], '{"STAY":{"type":"apartment","near":"hospital_cmc_vellore"}, "COOK":{"diet":"medical"}}');

-- Priority 40 — GROUP COMPOSITION
INSERT INTO bookings.trip_intent_rules (id, rule_name, priority, trigger_type, trigger_value, inferred_intent, suggested_verticals, vertical_filters)
VALUES
  (gen_random_uuid(), 'Couple leisure trip', 40, 'GROUP',
   '{"pax":2,"age_range":"couple","destination_type":"leisure"}', 'LEISURE',
   ARRAY['STAY','SPA','CAB'], '{"STAY":{"tier":"premium"}}'),
  (gen_random_uuid(), 'Large family non-leisure', 40, 'GROUP',
   '{"min_pax":4,"mixed_age":true,"non_leisure_destination":true}', 'FAMILY',
   ARRAY['STAY','COOK','CAB'], '{"STAY":{"type":"apartment"}}'),
  (gen_random_uuid(), 'Solo business — short', 40, 'GROUP',
   '{"pax":1,"max_duration_days":2}', 'BUSINESS',
   ARRAY['STAY','CAB','INSURANCE'], '{"STAY":{"type":"hotel"}}'),
  (gen_random_uuid(), 'Solo leisure', 40, 'GROUP',
   '{"pax":1,"min_duration_days":3,"destination_type":"leisure"}', 'LEISURE',
   ARRAY['STAY','EXPERIENCE','CAB'], '{}');

-- Priority 50 — EDUCATION / OTHER (lower priority specific patterns)
INSERT INTO bookings.trip_intent_rules (id, rule_name, priority, trigger_type, trigger_value, inferred_intent, suggested_verticals, vertical_filters)
VALUES
  (gen_random_uuid(), 'College visit pattern', 50, 'COMPOUND',
   '{"min_pax":2,"max_pax":3,"month":[5,6,7]}', 'EDUCATION',
   ARRAY['STAY','CAB'], '{"STAY":{"type":"hotel"}}'),
  (gen_random_uuid(), 'PG move-in pattern', 50, 'COMPOUND',
   '{"user_flag":"new_pg_signup","destination_type":"metro"}', 'MOVE_IN',
   ARRAY['STAY','CAB','COOK'], '{"STAY":{"max_nights":7}}');

-- Priority 99 — FALLBACK (last-resort match for everything unclassified)
INSERT INTO bookings.trip_intent_rules (id, rule_name, priority, trigger_type, trigger_value, inferred_intent, suggested_verticals, vertical_filters)
VALUES
  (gen_random_uuid(), 'Fallback — anything else', 99, 'FALLBACK',
   '{}', 'UNCLASSIFIED',
   ARRAY['STAY'], '{}');
