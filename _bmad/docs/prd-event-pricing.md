# PRD: Chef Event Pricing Engine

## 1. Market Research & Competitive Analysis

### Indian Platforms

| Platform | Base Pricing | Model | Add-ons | Commission | Min Order |
|----------|-------------|-------|---------|------------|-----------|
| **COOX.in** | Per-person + per-dish matrix (starts ~₹249/person) | Chef sets nothing — platform calculates price from (guests x dishes). 1 Chef for small parties, auto-adds Assistant Cook for large. | Live counters (Dosa/BBQ/Pasta etc.) priced as flat add-ons. Decoration, crockery, staff extra. | ~20-25% (estimated, not public) | ₹999 booking |
| **ChefDarbari** | ₹2,500-3,000 base for ≤15 guests | Refundable ₹1,999 booking fee + chef charges (menu complexity × guests). Ingredients separate. 40% advance, 60% after cooking. | Helper cook, extra courses, premium ingredients | Not public | ₹2,500 |
| **LookMyCook** | Per-event flat rate | Chef profiles with fixed per-event pricing. Customer picks chef → gets quote. | Helper, specialty items | Commission-based | Varies by chef |
| **Urban Company** | Platform-controlled fixed pricing | UC sets all prices. Cook/chef has no control. Fixed per-service rates. | Limited — standard packages only | 28% commission | Service minimum |
| **Sulekha** | Lead-gen model | Connects customer with cooks. No transaction pricing — cooks set own rates offline. | N/A | Charges cook for leads | N/A |

### International Platforms

| Platform | Base Pricing | Model | Commission | Key Insight |
|----------|-------------|-------|------------|-------------|
| **Yhangry (UK)** | Chef-controlled per-person (£35-250/pp) | Chef sets menu + price. Customer requests → multiple chefs bid with menus & quotes. All-inclusive (ingredients + cooking + cleanup). | ~25-30% | **Quote-based bidding** — chefs compete for events |
| **Take a Chef** | Per-person ($50-300/pp) | Chef creates menu proposals. Platform suggests price range. Customer picks. | ~25% | Global, multi-currency, menu proposal system |
| **CookinGenie (US)** | $95-250/person all-inclusive | Platform sets price ranges by tier (casual/fine dining). Chef selects tier. Groceries + cooking + cleanup included. | Not public | **Tier-based** — casual, premium, fine dining |

### Key Patterns Identified

1. **Price = f(guests, dishes, complexity, chef tier)** — every platform uses some combination
2. **Two dominant models**: Platform-controlled (UC, CookinGenie) vs Chef-controlled (Yhangry, Take a Chef)
3. **COOX (our closest competitor)**: Hybrid — platform calculates from a formula, chef doesn't set price
4. **Add-ons are always flat-rate** — live counters, decoration, staff never priced per-person
5. **Per-person pricing wins** for events (easier for customer to understand)
6. **Ingredients**: Indian platforms keep separate, international platforms bundle in
7. **No one does dynamic/surge pricing** for chef services
8. **Minimum order/spend** is universal — prevents uneconomical small bookings

---

## 2. Problem Statement

Safar Cooks event pricing is fully hardcoded in the frontend:
- Per-plate rate: ₹300 (fixed, no chef variation)
- Live counters: 5 items with static prices
- Add-ons: 5 items with static prices
- Staff: ₹1,500/person (fixed)
- Platform fee: 10% (fixed)

**Problems:**
1. Admin cannot adjust prices without code deployment
2. Chefs cannot differentiate on pricing (premium vs budget)
3. No per-city pricing (Mumbai vs Jaipur cost of living differs 2-3x)
4. Cannot A/B test pricing or run promotions
5. No min/max guardrails

---

## 3. Proposed Solution

### Pricing Architecture (Hybrid Model — inspired by COOX + Yhangry)

**Three tiers of pricing control:**

```
Admin (Platform Defaults)
  └── City Overrides (optional)
       └── Chef Overrides (optional, within min/max bounds)
```

**Resolution order:** Chef price > City price > Platform default

### Pricing Categories

| Category | Type | Examples |
|----------|------|---------|
| `BASE_CONFIG` | Per-plate/per-person | Food per plate, staff per person, platform fee % |
| `LIVE_COUNTER` | Flat per counter | Dosa, Pasta, BBQ, Chaat, Tandoor |
| `ADDON` | Flat per item | Decoration, Cake, Crockery, Appliances, Table Setup |
| `CHEF_TIER` | Multiplier | BUDGET (0.8x), STANDARD (1.0x), PREMIUM (1.5x), CELEBRITY (2.5x) |

### Chef Tier System (inspired by CookinGenie)

| Tier | Multiplier | Description |
|------|-----------|-------------|
| BUDGET | 0.8x | Home cooks, basic meals |
| STANDARD | 1.0x | Experienced cooks, diverse menus |
| PREMIUM | 1.5x | Restaurant-trained, specialty cuisines |
| CELEBRITY | 2.5x | TV chefs, Michelin-experienced, influencer chefs |

**Final price = base_price × tier_multiplier × city_factor**

### Minimum Order Value

| Tier | Min Event Value |
|------|----------------|
| BUDGET | ₹2,500 |
| STANDARD | ₹5,000 |
| PREMIUM | ₹10,000 |
| CELEBRITY | ₹25,000 |

---

## 4. Data Model

### Table: `event_pricing_defaults`
```sql
CREATE TABLE event_pricing_defaults (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category        VARCHAR(20) NOT NULL,  -- BASE_CONFIG, LIVE_COUNTER, ADDON
    item_key        VARCHAR(50) NOT NULL UNIQUE,
    label           VARCHAR(100) NOT NULL,
    description     VARCHAR(255),
    icon            VARCHAR(10),
    default_price_paise BIGINT NOT NULL,
    price_type      VARCHAR(20) NOT NULL,  -- FIXED, PER_PERSON, PER_PLATE, PERCENTAGE
    min_price_paise BIGINT,                -- floor for chef override
    max_price_paise BIGINT,                -- ceiling for chef override
    sort_order      INT DEFAULT 0,
    active          BOOLEAN DEFAULT true,
    created_at      TIMESTAMP DEFAULT now(),
    updated_at      TIMESTAMP DEFAULT now()
);
```

### Table: `city_pricing_overrides`
```sql
CREATE TABLE city_pricing_overrides (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    city            VARCHAR(100) NOT NULL,
    item_key        VARCHAR(50) NOT NULL REFERENCES event_pricing_defaults(item_key),
    price_paise     BIGINT NOT NULL,
    city_factor     DECIMAL(3,2),          -- e.g., 1.3 for Mumbai, 0.8 for Jaipur
    active          BOOLEAN DEFAULT true,
    UNIQUE(city, item_key)
);
```

### Table: `chef_event_pricing`
```sql
CREATE TABLE chef_event_pricing (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chef_id             UUID NOT NULL,
    item_key            VARCHAR(50) NOT NULL REFERENCES event_pricing_defaults(item_key),
    custom_price_paise  BIGINT NOT NULL,
    available           BOOLEAN DEFAULT true,  -- chef can disable items they don't offer
    created_at          TIMESTAMP DEFAULT now(),
    updated_at          TIMESTAMP DEFAULT now(),
    UNIQUE(chef_id, item_key)
);
```

### Table: `chef_tier_config`
```sql
-- Stored in chef_profiles table as new columns:
ALTER TABLE chef_profiles ADD COLUMN chef_tier VARCHAR(20) DEFAULT 'STANDARD';
ALTER TABLE chef_profiles ADD COLUMN min_event_value_paise BIGINT DEFAULT 500000;
```

---

## 5. API Design

### Public APIs
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/chef-events/pricing` | Platform defaults (for generic event page) |
| GET | `/api/v1/chef-events/pricing?chefId=x` | Merged pricing for specific chef |
| GET | `/api/v1/chef-events/pricing?chefId=x&city=Mumbai` | Chef + city resolved pricing |
| GET | `/api/v1/chef-events/pricing/tiers` | Available chef tiers with multipliers |

### Chef APIs (authenticated)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/chef-events/pricing/me` | Chef's current custom pricing |
| PUT | `/api/v1/chef-events/pricing/me` | Bulk update chef's custom pricing |
| PUT | `/api/v1/chef-events/pricing/me/{itemKey}` | Update single item price |
| DELETE | `/api/v1/chef-events/pricing/me/{itemKey}` | Reset to platform default |

### Admin APIs
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/chef-events/pricing/admin` | List all defaults |
| POST | `/api/v1/chef-events/pricing/admin` | Create new pricing item |
| PUT | `/api/v1/chef-events/pricing/admin/{itemKey}` | Update default price |
| DELETE | `/api/v1/chef-events/pricing/admin/{itemKey}` | Deactivate item |
| GET | `/api/v1/chef-events/pricing/admin/cities` | List city overrides |
| PUT | `/api/v1/chef-events/pricing/admin/cities/{city}` | Set city pricing |

---

## 6. Price Resolution Algorithm

```
function resolvePrice(itemKey, chefId, city):
    default = event_pricing_defaults[itemKey]
    
    if chefId and chef_event_pricing[chefId][itemKey] exists:
        price = chef_event_pricing.custom_price_paise
        // Enforce bounds
        price = clamp(price, default.min_price_paise, default.max_price_paise)
    elif city and city_pricing_overrides[city][itemKey] exists:
        price = city_pricing_overrides.price_paise
    else:
        price = default.default_price_paise
    
    // Apply chef tier multiplier
    if chefId:
        tier = chef_profiles[chefId].chef_tier
        price = price * TIER_MULTIPLIERS[tier]
    
    return price
```

---

## 7. Seed Data

```sql
-- Base configs
INSERT INTO event_pricing_defaults VALUES
('BASE_CONFIG','per_plate','Per Plate Food','Base food cost per guest','🍛',30000,'PER_PLATE',15000,100000,1,true),
('BASE_CONFIG','staff','Extra Serving Staff','Waiters for serving & cleanup','🧑‍🍳',150000,'PER_PERSON',100000,300000,2,true),
('BASE_CONFIG','platform_fee_pct','Platform Fee','Service fee percentage','💰',1000,'PERCENTAGE',500,2000,3,true),

-- Live counters
('LIVE_COUNTER','dosa','Live Dosa Counter','South Indian dosa station','🥞',300000,'FIXED',150000,500000,1,true),
('LIVE_COUNTER','pasta','Live Pasta Counter','Italian pasta station','🍝',350000,'FIXED',200000,600000,2,true),
('LIVE_COUNTER','bbq','Live BBQ Counter','Barbecue grill station','🔥',500000,'FIXED',300000,800000,3,true),
('LIVE_COUNTER','chaat','Live Chaat Counter','Street food chaat station','🥗',250000,'FIXED',150000,400000,4,true),
('LIVE_COUNTER','tandoor','Live Tandoor Counter','Tandoori grill station','🫓',400000,'FIXED',200000,700000,5,true),

-- Add-ons
('ADDON','decoration','Event Decoration','Balloons, banners, table setting & theme decor','🎈',500000,'FIXED',200000,1500000,1,true),
('ADDON','cake','Designer Cake','Custom theme cake (1-2 kg)','🎂',200000,'FIXED',100000,500000,2,true),
('ADDON','crockery','Crockery Rental','Plates, glasses, cutlery, serving bowls','🍽️',80000,'FIXED',50000,200000,3,true),
('ADDON','appliances','Appliance Rental','Chafing dishes, gas stoves, induction','🔌',50000,'FIXED',30000,150000,4,true),
('ADDON','table_setup','Fine Dine Table Setup','Premium tablecloth, candles, flowers','🕯️',80000,'FIXED',50000,200000,5,true);

-- City overrides (Tier 1 cities get premium)
INSERT INTO city_pricing_overrides (city, item_key, price_paise, city_factor) VALUES
('Mumbai', 'per_plate', 40000, 1.30),
('Delhi', 'per_plate', 38000, 1.25),
('Bangalore', 'per_plate', 35000, 1.15),
('Hyderabad', 'per_plate', 32000, 1.05),
('Chennai', 'per_plate', 32000, 1.05),
('Kolkata', 'per_plate', 28000, 0.95),
('Pune', 'per_plate', 33000, 1.10);
```

---

## 8. Frontend Changes

### Event Booking Page (`/cooks/events`)
- Replace hardcoded `LIVE_COUNTERS` and `ADDONS` arrays with API call
- `GET /api/v1/chef-events/pricing?chefId={selected}&city={detected}`
- Render dynamically from response
- Show chef tier badge and any price adjustments

### Chef Dashboard (new "Event Pricing" tab)
- Show all pricing items with platform defaults
- Allow chef to set custom prices (within min/max)
- Toggle items on/off (e.g., chef doesn't offer BBQ counter)
- Preview: "Customers in Mumbai will see: ₹X"

### Admin Portal (new "Event Pricing" page)
- CRUD for pricing items
- City override management
- View chef-level overrides
- Bulk update (e.g., increase all counter prices by 10%)

---

## 9. Implementation Phases

### Phase 1 (MVP) — Platform Defaults
- `event_pricing_defaults` table + Flyway migration
- Admin CRUD API
- Frontend fetches from API instead of hardcoded
- **Effort: 2-3 hours**

### Phase 2 — Chef Overrides
- `chef_event_pricing` table
- Chef pricing API
- Chef dashboard "Event Pricing" tab
- **Effort: 2-3 hours**

### Phase 3 — City Pricing + Tiers
- `city_pricing_overrides` table
- Chef tier column + multiplier logic
- City auto-detection on frontend
- **Effort: 2-3 hours**

---

## 10. Success Metrics

| Metric | Baseline | Target |
|--------|----------|--------|
| Event booking conversion | N/A (new feature) | 5% of visitors |
| Average event value | ₹8,000 (hardcoded) | ₹12,000 (with upsells) |
| Chef pricing adoption | 0% | 30% of chefs customize |
| Add-on attach rate | Unknown | 40% of bookings include ≥1 add-on |

---

## Sources

- [COOX.in — Book a Cook](https://www.coox.in/cook)
- [COOX.in — How It Works](https://www.coox.in/cook/how-it-works)
- [Yhangry — Private Chef Hire](https://yhangry.com/en-us/)
- [ChefDarbari — House Party Chef](https://www.chefdarbari.com/)
- [Urban Company Business Model](https://oyelabs.com/urbanclap-business-model/)
- [Urban Company Revenue Model](https://whiteocean.in/blog/urban-company-revenue-model/)
- [CookinGenie — How Much Does a Private Chef Cost](https://cookingenie.com/content/blog/hiring-a-personal-chef-what-to-expect-in-terms-of-cost/)
- [Take a Chef — Private Chef Cost](https://www.takeachef.com/blog/en/how-much-does-a-private-chef-cost-per-month)
- [LookMyCook — Chef for Party](https://lookmycook.com/know-your-chef)
