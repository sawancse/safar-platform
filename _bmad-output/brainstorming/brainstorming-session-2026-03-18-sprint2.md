# Brainstorming Session: Safar Platform Sprint 2 — All Pending Features
**Date:** 2026-03-18 (Evening Session)
**Techniques:** Morphological Analysis, First Principles Thinking, SCAMPER
**Scope:** 7 features — AI Autopilot, Aashray Phase 2, Mobile SDKs, Channel Manager, PG Tenancy, Reverse Search, Locality Polygons

---

## Theme 1: AI Autopilot (Dynamic Pricing + Smart Messaging + Calendar Optimization)

### Morphological Analysis
| Dimension | Options |
|-----------|---------|
| Pricing Signal | Demand history · Competitor rates · Season · Day-of-week · Events · Lead time |
| Algorithm | Rule-based · ML regression · Reinforcement learning · Hybrid rules+ML |
| Granularity | Per-night · Per-room-type · Per-hour (hourly stays) · Per-channel |
| Auto-apply | Manual review · One-click apply · Full autopilot · Threshold-based |
| Messaging | Template library · Context-aware generation · Sentiment analysis · Multi-lingual |
| Calendar Opt | Gap-fill discounts · Min-stay rules · Revenue projection · Maintenance windows |

### First Principles
- **Why dynamic pricing?** Fixed prices leave money on table during peak and lose bookings during low demand
- **India-specific**: Festival calendar drives 40%+ of travel demand spikes (Diwali, Holi, long weekends)
- **PG pricing is different**: Monthly rent rarely changes, but security deposit and package pricing can be dynamic
- **Messaging ROI**: 80% of host-guest messages are repetitive (directions, check-in, wifi) — automate these

### SCAMPER
- **Substitute**: Replace manual pricing with AI suggestions → 15-20% revenue lift (Airbnb data)
- **Combine**: Merge pricing + calendar in one "Revenue Manager" view
- **Adapt**: Adopt airline yield management for hotels (Safar's hourly stays = airline seats)
- **Put to other use**: Use pricing data to power host onboarding ("earn ₹X/month" estimates)
- **Eliminate**: Remove guesswork — show hosts exactly why price X is recommended
- **Reverse**: Let guests bid on off-peak dates (future feature)

### Selected Approach
- **Phase 1 (now)**: Rule-based pricing with factor scoring (weekend, season, demand, events, competitors)
- **Phase 2 (future)**: ML model trained on actual booking data after 6 months of operation
- Smart messaging: Template library + context-aware suggestions (no LLM needed yet)
- Calendar optimizer: Statistical analysis of gaps, lead times, and revenue projections

---

## Theme 2: Aashray Phase 2 (Case Management + Smart Matching + Languages)

### Morphological Analysis
| Dimension | Options |
|-----------|---------|
| Case Source | NGO referral · Self-registration · Government · UN agencies |
| Matching | Manual NGO · Algorithm (score-based) · AI matching · Hybrid |
| Priority | Time-to-house SLA · Family size · Vulnerability score · Need-by date |
| Languages | Hindi/English (done) · Tamil · Bengali · Marathi · Odia · Assamese |
| Verification | NGO vouching · Aadhaar · Government ID · Self-declaration |

### First Principles
- **Speed matters**: Displaced persons need housing in 24-72 hours, not weeks
- **Trust gap**: Hosts need assurance (NGO backing, government guarantee)
- **Language barrier**: 50% of potential seekers don't speak Hindi/English fluently
- **Budget reality**: Many seekers have zero budget — 100% NGO-funded or Aashray commission waiver needed

### SCAMPER
- **Combine**: Merge case tracking with listing availability for real-time matching
- **Adapt**: Adopt UNHCR case management patterns for Indian context
- **Magnify**: Priority scoring → auto-escalation for CRITICAL cases (families with children/elderly)
- **Eliminate**: Remove landlord hesitation with Safar guarantee (damage deposit pool from NGO funding)

### Selected Approach
- AashrayCase entity with case number, priority, smart matching
- Match score (0-100) based on: city match, budget, family size, accessibility, language
- 3 new languages: Tamil, Bengali, Marathi (covers 300M+ additional speakers)
- Kafka events for real-time notification to NGOs and hosts

---

## Theme 3: Mobile Gaps (Razorpay + Google Sign-In + Push Notifications)

### Morphological Analysis
| Dimension | Options |
|-----------|---------|
| Payment | Native SDK · WebView checkout · Deep link · In-app browser |
| Auth | expo-auth-session · expo-google-sign-in · WebView OAuth · Native module |
| Push | Expo Push (managed) · Firebase FCM (direct) · OneSignal · Amazon SNS |
| Token Storage | SecureStore · AsyncStorage · Keychain/Keystore |

### First Principles
- **Expo managed workflow**: Can't use native Razorpay SDK without ejecting
- **WebView checkout**: Works in managed workflow, covers 95% of payment UX
- **expo-auth-session**: Official way to do OAuth in Expo, works with Google
- **expo-notifications**: Built-in push with Expo Push Service, no Firebase config needed

### Selected Approach
- Razorpay via react-native-webview (proven pattern, no ejection)
- Google Sign-In via expo-auth-session (official, managed workflow compatible)
- Push via expo-notifications + expo-device (Expo Push Service handles FCM/APNs)

---

## Theme 4: Channel Manager Phase 2 (Channex.io)

### Morphological Analysis
| Dimension | Options |
|-----------|---------|
| Platform | Channex.io · SiteMinder · RateGain · Custom per-OTA |
| Sync Mode | Push only · Pull only · Bi-directional · Event-driven |
| Frequency | Real-time webhook · 15-min poll · 30-min batch · On-demand |
| Conflict | Last-write-wins · Source priority · Manual resolution · Merge |

### First Principles
- **Why channel manager?** Hosts on 3+ OTAs waste hours on manual updates → errors → overbookings
- **Channex.io**: REST API, 200+ channels, ₹500-2000/month/property → pays for itself
- **India channels**: MMT, OYO, Agoda are critical; Airbnb/Booking.com already via iCal
- **Overbooking risk**: Real-time or near-real-time sync is essential

### SCAMPER
- **Combine**: Merge iCal sync + channel manager → single "Distribution" tab for hosts
- **Adapt**: Use Channex webhooks for instant booking notification (vs 15-min iCal poll)
- **Magnify**: Sync not just availability but rates and content (photos, description)

### Selected Approach
- Channex.io REST client with scheduled sync (30-min) + webhook for real-time bookings
- Room type mapping (local ↔ channel)
- Sync logs for transparency
- Property-level connection management

---

## Theme 5: PG Tenancy Model

### Morphological Analysis
| Dimension | Options |
|-----------|---------|
| Billing | Manual invoice · Razorpay subscription · NACH mandate · UPI autopay |
| Cycle | Monthly (standard) · Quarterly · Weekly |
| Notice | 30 days (standard) · 15 days · 60 days · Flexible |
| Deposits | 1 month · 2 months · Variable · Refundable/Non-refundable split |
| Add-ons | Fixed packages · Metered (electricity) · A-la-carte · Bundled tiers |

### First Principles
- **PG is subscription, not booking**: Monthly recurring vs one-time transaction
- **Bed-level granularity**: PGs sell beds, not rooms — need bed tracking
- **Invoice = trust**: Auto-generated monthly invoices build tenant trust + tax compliance
- **Notice period**: Legal requirement in most Indian states (30 days standard)

### Selected Approach
- PgTenancy entity with bed-level tracking, monthly billing cycle
- TenancyInvoice with auto-generation via scheduler
- Razorpay subscription ID for auto-debit
- Notice period management with automated moveOut calculation

---

## Theme 6: Reverse Search ("Looking For" Profiles)

### Morphological Analysis
| Dimension | Options |
|-----------|---------|
| Profile Type | PG seeker · Rental seeker · Flatmate seeker · Commercial tenant |
| Matching | Budget + location · Preferences · Scoring · Two-sided |
| Discovery | Host browsing · Push notification · Email digest · In-app feed |
| Privacy | Full profile visible · Anonymous until match · Name-only · Verified-only |

### First Principles
- **Supply-demand inversion**: PG seekers outnumber PG listings in most Indian cities
- **Host pain point**: Finding good tenants is as hard as finding a PG
- **Trust signals**: Occupation, company/college, gender, veg/non-veg preferences matter in India
- **Time sensitivity**: PG seekers need housing within 1-2 weeks of searching

### SCAMPER
- **Reverse**: Instead of seeker finding listing, listing finds seeker → push-based matching
- **Combine**: Merge with Aashray for refugee housing matching
- **Adapt**: Adopt dating app "mutual match" UX — both sides express interest

### Selected Approach
- SeekerProfile with Indian-specific fields (veg preference, occupation, gender policy)
- Two-way matching: seekers see matching listings, hosts see matching seekers
- Active/Matched/Expired lifecycle

---

## Theme 7: Locality Polygons (OpenStreetMap)

### Morphological Analysis
| Dimension | Options |
|-----------|---------|
| Source | OpenStreetMap · Google Places · MapmyIndia · Municipal data |
| Storage | PostGIS · GeoJSON in TEXT column · Elasticsearch geo_shape · MongoDB |
| Query | JTS point-in-polygon · PostGIS ST_Contains · ES geo_shape · Turf.js |
| Display | Leaflet GeoJSON · Mapbox · Google Maps · SVG overlay |

### First Principles
- **Why polygons?** Radius search is imprecise — "Koramangala" is an area, not a circle
- **OSM is free**: Nominatim API with proper rate limiting (1 req/sec)
- **India OSM coverage**: Major city neighborhoods well-mapped, rural areas less so
- **Hybrid approach**: Polygon when available, radius fallback when not

### Selected Approach
- Nominatim API for polygon fetch, store as GeoJSON TEXT in PostgreSQL
- JTS for server-side point-in-polygon tests
- ES geo_shape for search-time filtering
- Leaflet GeoJSON layer for frontend visualization
- Seed 40+ key localities across Bangalore, Mumbai, Delhi, Goa

---

## Implementation Priority Matrix

| Feature | Impact | Effort | Priority |
|---------|--------|--------|----------|
| AI Dynamic Pricing | HIGH (revenue) | MEDIUM | P0 |
| PG Tenancy Model | HIGH (retention) | MEDIUM | P0 |
| Channel Manager | HIGH (distribution) | HIGH | P0 |
| Reverse Search | HIGH (PG growth) | LOW | P1 |
| Locality Polygons | MEDIUM (search UX) | MEDIUM | P1 |
| Aashray Phase 2 | MEDIUM (social impact) | MEDIUM | P1 |
| Mobile SDKs | MEDIUM (conversion) | LOW | P1 |

## Total New Files Estimated: ~45 backend + ~15 frontend + 3 i18n + 5 migrations
## Total New Endpoints: ~60+ across all services
