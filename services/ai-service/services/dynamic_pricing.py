"""
Dynamic Pricing Engine for Safar Platform.
Rule-based pricing with multi-factor scoring: weekend, season, demand, events, competitors.
All monetary values in paise (1 INR = 100 paise).
"""

from datetime import date, datetime, timedelta
from enum import Enum
from typing import Optional

from fastapi import APIRouter, Query, HTTPException
from pydantic import BaseModel, Field
import httpx

router = APIRouter(prefix="/api/v1/ai/pricing", tags=["Dynamic Pricing"])

# ──────────────────────────────────────────────
# Indian Festival & Event Calendar 2026
# ──────────────────────────────────────────────
EVENTS_2026: dict[str, list[tuple[date, date, str, float]]] = {
    # (start, end, name, demand_multiplier)
    "national": [
        (date(2026, 1, 26), date(2026, 1, 26), "Republic Day", 1.15),
        (date(2026, 3, 17), date(2026, 3, 18), "Holi", 1.30),
        (date(2026, 8, 15), date(2026, 8, 15), "Independence Day", 1.15),
        (date(2026, 9, 7), date(2026, 9, 7), "Ganesh Chaturthi", 1.20),
        (date(2026, 10, 2), date(2026, 10, 2), "Gandhi Jayanti", 1.10),
        (date(2026, 10, 7), date(2026, 10, 11), "Navratri", 1.25),
        (date(2026, 10, 7), date(2026, 10, 11), "Durga Puja", 1.25),
        (date(2026, 10, 20), date(2026, 10, 22), "Diwali", 1.40),
        (date(2026, 12, 25), date(2026, 12, 26), "Christmas", 1.25),
        (date(2026, 12, 31), date(2027, 1, 1), "New Year", 1.35),
    ],
    "long_weekends": [
        (date(2026, 1, 24), date(2026, 1, 26), "Republic Day Weekend", 1.20),
        (date(2026, 3, 14), date(2026, 3, 18), "Holi Weekend", 1.25),
        (date(2026, 8, 14), date(2026, 8, 16), "Independence Day Weekend", 1.20),
        (date(2026, 10, 1), date(2026, 10, 4), "Gandhi Jayanti Weekend", 1.15),
    ],
    "school_holidays": [
        (date(2026, 5, 1), date(2026, 6, 15), "Summer Vacation", 1.15),
        (date(2026, 10, 5), date(2026, 10, 20), "Dussehra-Diwali Break", 1.20),
        (date(2026, 12, 20), date(2027, 1, 5), "Winter Break", 1.20),
    ],
}

# City-specific peak seasons (month ranges → multiplier)
CITY_SEASONS: dict[str, list[tuple[int, int, float, str]]] = {
    # (start_month, end_month, multiplier, label)
    "Goa":       [(10, 2, 1.30, "Peak Season"), (3, 5, 1.10, "Shoulder Season")],
    "Manali":    [(3, 6, 1.25, "Summer Rush"), (12, 2, 1.30, "Snow Season")],
    "Shimla":    [(3, 6, 1.25, "Summer Rush"), (12, 2, 1.20, "Winter")],
    "Jaipur":    [(10, 3, 1.20, "Winter Tourism")],
    "Udaipur":   [(10, 3, 1.25, "Winter Tourism")],
    "Kerala":    [(9, 3, 1.20, "Backwater Season")],
    "Munnar":    [(9, 3, 1.20, "Tea Garden Season")],
    "Coorg":     [(10, 3, 1.15, "Coffee Season")],
    "Mumbai":    [(10, 12, 1.10, "Festival Season")],
    "Delhi":     [(10, 12, 1.10, "Festival Season")],
    "Bangalore": [(10, 12, 1.10, "Festival Season")],
}

# Property type base multipliers
PROPERTY_MULTIPLIERS: dict[str, float] = {
    "HOTEL": 1.0, "VILLA": 1.15, "HOME": 1.0, "APARTMENT": 0.95,
    "COTTAGE": 1.10, "GUESTHOUSE": 0.90, "ROOM": 0.80,
    "PG": 0.70, "COMMERCIAL": 1.05, "UNIQUE": 1.20,
    "BUDGET_HOTEL": 0.85, "MEDICAL": 1.0,
}

# City base demand index (0-1)
CITY_BASE_DEMAND: dict[str, float] = {
    "Mumbai": 0.80, "Delhi": 0.75, "Bangalore": 0.75, "Goa": 0.70,
    "Jaipur": 0.65, "Udaipur": 0.65, "Manali": 0.60, "Chennai": 0.70,
    "Hyderabad": 0.68, "Pune": 0.65, "Kolkata": 0.62, "Kochi": 0.60,
    "Coorg": 0.55, "Shimla": 0.58, "Ooty": 0.55,
}


# ──────────────────────────────────────────────
# Models
# ──────────────────────────────────────────────
class PricingFactor(BaseModel):
    name: str
    adjustment_percent: float
    description: str


class PricingSuggestion(BaseModel):
    listing_id: str
    current_price_paise: int
    suggested_price_paise: int
    factors: list[PricingFactor]
    confidence_score: float = Field(ge=0, le=1)
    valid_for_date: str


class BulkPricingRequest(BaseModel):
    listings: list[dict]  # [{listing_id, base_price_paise, city, property_type}]
    target_date: str  # YYYY-MM-DD


class PricingRule(BaseModel):
    listing_id: str
    min_price_paise: int
    max_price_paise: int
    aggressiveness: str = "moderate"  # conservative, moderate, aggressive
    auto_apply: bool = False
    weekend_boost_percent: float = 20.0
    low_demand_discount_percent: float = 10.0


class CalendarPrice(BaseModel):
    date: str
    base_price_paise: int
    suggested_price_paise: int
    factors: list[PricingFactor]
    is_event_day: bool
    event_name: Optional[str] = None


class PricingCalendarResponse(BaseModel):
    listing_id: str
    prices: list[CalendarPrice]
    avg_suggested_paise: int
    max_suggested_paise: int
    min_suggested_paise: int
    potential_revenue_increase_percent: float


class PricingAnalytics(BaseModel):
    listing_id: str
    current_avg_price_paise: int
    market_avg_price_paise: int
    suggested_avg_price_paise: int
    price_position: str  # "below_market", "at_market", "above_market"
    revenue_potential_monthly_paise: int
    occupancy_estimate_percent: float
    top_factors: list[PricingFactor]


# In-memory pricing rules store (would be DB in production)
_pricing_rules: dict[str, PricingRule] = {}


# ──────────────────────────────────────────────
# Core Pricing Algorithm
# ──────────────────────────────────────────────
def _is_month_in_range(month: int, start: int, end: int) -> bool:
    if start <= end:
        return start <= month <= end
    return month >= start or month <= end


def _get_event_for_date(d: date) -> tuple[Optional[str], float]:
    for category in EVENTS_2026.values():
        for start, end, name, mult in category:
            if start <= d <= end:
                return name, mult
    return None, 1.0


def _get_season_factor(city: str, d: date) -> tuple[float, str]:
    seasons = CITY_SEASONS.get(city, [])
    for start_m, end_m, mult, label in seasons:
        if _is_month_in_range(d.month, start_m, end_m):
            return mult, label
    return 1.0, "Off-season"


def _get_weekend_factor(d: date) -> float:
    return 1.20 if d.weekday() >= 4 else 1.0  # Fri, Sat, Sun


def _get_demand_factor(city: str, occupancy_rate: float) -> float:
    base = CITY_BASE_DEMAND.get(city, 0.60)
    combined = (base + occupancy_rate) / 2
    if combined > 0.80:
        return 1.15
    elif combined > 0.60:
        return 1.05
    elif combined < 0.40:
        return 0.90
    return 1.0


def calculate_price(
    base_price_paise: int,
    city: str,
    property_type: str,
    target_date: date,
    occupancy_rate: float = 0.65,
    aggressiveness: str = "moderate",
) -> tuple[int, list[PricingFactor], float]:
    factors: list[PricingFactor] = []
    multiplier = 1.0

    # 1. Weekend factor
    wf = _get_weekend_factor(target_date)
    if wf != 1.0:
        adj = (wf - 1.0) * 100
        factors.append(PricingFactor(
            name="weekend", adjustment_percent=adj,
            description=f"Weekend premium ({target_date.strftime('%A')})"
        ))
        multiplier *= wf

    # 2. Season factor
    sf, season_label = _get_season_factor(city, target_date)
    if sf != 1.0:
        adj = (sf - 1.0) * 100
        factors.append(PricingFactor(
            name="season", adjustment_percent=adj,
            description=f"{season_label} in {city}"
        ))
        multiplier *= sf

    # 3. Event factor
    event_name, ef = _get_event_for_date(target_date)
    if event_name:
        adj = (ef - 1.0) * 100
        factors.append(PricingFactor(
            name="event", adjustment_percent=adj,
            description=f"{event_name}"
        ))
        multiplier *= ef

    # 4. Demand factor
    df = _get_demand_factor(city, occupancy_rate)
    if df != 1.0:
        adj = (df - 1.0) * 100
        label = "High demand" if df > 1.0 else "Low demand discount"
        factors.append(PricingFactor(
            name="demand", adjustment_percent=adj,
            description=f"{label} ({city}, {occupancy_rate:.0%} occupancy)"
        ))
        multiplier *= df

    # 5. Property type factor
    pf = PROPERTY_MULTIPLIERS.get(property_type, 1.0)
    if pf != 1.0:
        adj = (pf - 1.0) * 100
        factors.append(PricingFactor(
            name="property_type", adjustment_percent=adj,
            description=f"{property_type} type adjustment"
        ))
        multiplier *= pf

    # Aggressiveness scaling
    agg_scale = {"conservative": 0.6, "moderate": 1.0, "aggressive": 1.4}
    scale = agg_scale.get(aggressiveness, 1.0)
    # Scale the deviation from 1.0
    adjusted_mult = 1.0 + (multiplier - 1.0) * scale

    suggested = int(base_price_paise * adjusted_mult)

    # Apply min/max from rules if available
    confidence = min(0.95, 0.5 + len(factors) * 0.1)

    return suggested, factors, confidence


# ──────────────────────────────────────────────
# Endpoints
# ──────────────────────────────────────────────
@router.post("/suggest", response_model=PricingSuggestion)
async def suggest_price(
    listing_id: str = Query(...),
    base_price_paise: int = Query(..., ge=100),
    city: str = Query(...),
    property_type: str = Query("HOME"),
    target_date: str = Query(..., description="YYYY-MM-DD"),
    occupancy_rate: float = Query(0.65, ge=0, le=1),
):
    d = date.fromisoformat(target_date)
    rule = _pricing_rules.get(listing_id)
    agg = rule.aggressiveness if rule else "moderate"

    suggested, factors, confidence = calculate_price(
        base_price_paise, city, property_type, d, occupancy_rate, agg
    )

    if rule:
        suggested = max(rule.min_price_paise, min(rule.max_price_paise, suggested))

    return PricingSuggestion(
        listing_id=listing_id,
        current_price_paise=base_price_paise,
        suggested_price_paise=suggested,
        factors=factors,
        confidence_score=confidence,
        valid_for_date=target_date,
    )


@router.post("/bulk-suggest", response_model=list[PricingSuggestion])
async def bulk_suggest(request: BulkPricingRequest):
    d = date.fromisoformat(request.target_date)
    results = []
    for item in request.listings:
        lid = item.get("listing_id", "")
        base = item.get("base_price_paise", 200000)
        city = item.get("city", "Mumbai")
        ptype = item.get("property_type", "HOME")
        occ = item.get("occupancy_rate", 0.65)

        suggested, factors, confidence = calculate_price(base, city, ptype, d, occ)
        results.append(PricingSuggestion(
            listing_id=lid,
            current_price_paise=base,
            suggested_price_paise=suggested,
            factors=factors,
            confidence_score=confidence,
            valid_for_date=request.target_date,
        ))
    return results


@router.get("/calendar/{listing_id}", response_model=PricingCalendarResponse)
async def pricing_calendar(
    listing_id: str,
    base_price_paise: int = Query(..., ge=100),
    city: str = Query(...),
    property_type: str = Query("HOME"),
    occupancy_rate: float = Query(0.65, ge=0, le=1),
    days: int = Query(30, ge=7, le=90),
):
    today = date.today()
    rule = _pricing_rules.get(listing_id)
    agg = rule.aggressiveness if rule else "moderate"
    prices: list[CalendarPrice] = []

    for i in range(days):
        d = today + timedelta(days=i)
        suggested, factors, _ = calculate_price(
            base_price_paise, city, property_type, d, occupancy_rate, agg
        )
        if rule:
            suggested = max(rule.min_price_paise, min(rule.max_price_paise, suggested))

        event_name, _ = _get_event_for_date(d)
        prices.append(CalendarPrice(
            date=d.isoformat(),
            base_price_paise=base_price_paise,
            suggested_price_paise=suggested,
            factors=factors,
            is_event_day=event_name is not None,
            event_name=event_name,
        ))

    suggested_values = [p.suggested_price_paise for p in prices]
    avg_suggested = sum(suggested_values) // len(suggested_values)
    base_total = base_price_paise * days
    suggested_total = sum(suggested_values)
    increase_pct = ((suggested_total - base_total) / base_total) * 100 if base_total > 0 else 0

    return PricingCalendarResponse(
        listing_id=listing_id,
        prices=prices,
        avg_suggested_paise=avg_suggested,
        max_suggested_paise=max(suggested_values),
        min_suggested_paise=min(suggested_values),
        potential_revenue_increase_percent=round(increase_pct, 1),
    )


@router.post("/rules", response_model=PricingRule)
async def create_pricing_rule(rule: PricingRule):
    if rule.min_price_paise >= rule.max_price_paise:
        raise HTTPException(400, "min_price must be less than max_price")
    if rule.aggressiveness not in ("conservative", "moderate", "aggressive"):
        raise HTTPException(400, "aggressiveness must be conservative, moderate, or aggressive")
    _pricing_rules[rule.listing_id] = rule
    return rule


@router.get("/rules/{listing_id}", response_model=Optional[PricingRule])
async def get_pricing_rule(listing_id: str):
    rule = _pricing_rules.get(listing_id)
    if not rule:
        raise HTTPException(404, "No pricing rule found for this listing")
    return rule


@router.get("/analytics/{listing_id}", response_model=PricingAnalytics)
async def pricing_analytics(
    listing_id: str,
    base_price_paise: int = Query(..., ge=100),
    city: str = Query(...),
    property_type: str = Query("HOME"),
    occupancy_rate: float = Query(0.65, ge=0, le=1),
):
    today = date.today()
    monthly_prices = []
    for i in range(30):
        d = today + timedelta(days=i)
        suggested, _, _ = calculate_price(
            base_price_paise, city, property_type, d, occupancy_rate
        )
        monthly_prices.append(suggested)

    suggested_avg = sum(monthly_prices) // len(monthly_prices)
    market_avg = int(CITY_BASE_DEMAND.get(city, 0.6) * 500000)  # rough market estimate

    if base_price_paise < market_avg * 0.85:
        position = "below_market"
    elif base_price_paise > market_avg * 1.15:
        position = "above_market"
    else:
        position = "at_market"

    # Revenue projection (suggested avg × estimated occupancy × 30 days)
    occ = min(occupancy_rate + 0.05, 1.0)  # slight optimism with better pricing
    revenue = int(suggested_avg * occ * 30)

    _, top_factors, _ = calculate_price(
        base_price_paise, city, property_type, today, occupancy_rate
    )

    return PricingAnalytics(
        listing_id=listing_id,
        current_avg_price_paise=base_price_paise,
        market_avg_price_paise=market_avg,
        suggested_avg_price_paise=suggested_avg,
        price_position=position,
        revenue_potential_monthly_paise=revenue,
        occupancy_estimate_percent=round(occ * 100, 1),
        top_factors=top_factors,
    )


# ──────────────────────────────────────────────
# Competitor Comparison & Occupancy Auto-Pricing
# ──────────────────────────────────────────────


class CompetitorComparison(BaseModel):
    listing_id: str
    your_price_paise: int
    market_low_paise: int
    market_median_paise: int
    market_high_paise: int
    percentile: int  # where your price falls (0-100)
    recommendation: str  # "competitive", "underpriced", "overpriced"
    suggested_range_min_paise: int
    suggested_range_max_paise: int


class OccupancyPricingRequest(BaseModel):
    listing_id: str
    base_price_paise: int
    city: str
    property_type: str = "HOME"
    current_occupancy_percent: float = Field(ge=0, le=100)
    target_occupancy_percent: float = Field(ge=0, le=100, default=75)
    days_ahead: int = Field(ge=1, le=90, default=30)


class OccupancyPricingResponse(BaseModel):
    listing_id: str
    current_occupancy_percent: float
    target_occupancy_percent: float
    prices: list[dict]  # [{date, price_paise, expected_occupancy}]
    avg_suggested_paise: int
    estimated_monthly_revenue_paise: int
    strategy: str


@router.get("/competitor-comparison/{listing_id}", response_model=CompetitorComparison)
async def competitor_comparison(
    listing_id: str,
    price_paise: int = Query(..., ge=100),
    city: str = Query(...),
    property_type: str = Query("HOME"),
    bedrooms: int = Query(1, ge=1),
):
    """Compare your listing price against estimated market rates."""
    base_demand = CITY_BASE_DEMAND.get(city, 0.6)
    type_mult = PROPERTY_MULTIPLIERS.get(property_type, 1.0)

    # Estimate market prices based on city demand and type
    base_market = int(base_demand * 600000 * type_mult)  # base market per night in paise
    bhk_factor = 1 + (bedrooms - 1) * 0.3
    market_median = int(base_market * bhk_factor)
    market_low = int(market_median * 0.65)
    market_high = int(market_median * 1.45)

    # Calculate percentile
    if price_paise <= market_low:
        percentile = int((price_paise / max(market_low, 1)) * 25)
    elif price_paise <= market_median:
        percentile = 25 + int(((price_paise - market_low) / max(market_median - market_low, 1)) * 25)
    elif price_paise <= market_high:
        percentile = 50 + int(((price_paise - market_median) / max(market_high - market_median, 1)) * 25)
    else:
        percentile = min(75 + int(((price_paise - market_high) / max(market_high, 1)) * 25), 99)

    if percentile < 20:
        rec = "underpriced"
    elif percentile > 80:
        rec = "overpriced"
    else:
        rec = "competitive"

    return CompetitorComparison(
        listing_id=listing_id,
        your_price_paise=price_paise,
        market_low_paise=market_low,
        market_median_paise=market_median,
        market_high_paise=market_high,
        percentile=percentile,
        recommendation=rec,
        suggested_range_min_paise=int(market_median * 0.85),
        suggested_range_max_paise=int(market_median * 1.15),
    )


@router.post("/occupancy-pricing", response_model=OccupancyPricingResponse)
async def occupancy_pricing(req: OccupancyPricingRequest):
    """Auto-adjust prices to hit target occupancy."""
    current_occ = req.current_occupancy_percent / 100
    target_occ = req.target_occupancy_percent / 100
    today = date.today()

    # Strategy: if current < target, lower prices; if current > target, raise prices
    occ_gap = target_occ - current_occ
    if occ_gap > 0.15:
        strategy = "aggressive_discount"
        price_adj = -0.15  # up to 15% discount
    elif occ_gap > 0.05:
        strategy = "moderate_discount"
        price_adj = -0.08
    elif occ_gap < -0.15:
        strategy = "aggressive_increase"
        price_adj = 0.20  # up to 20% increase
    elif occ_gap < -0.05:
        strategy = "moderate_increase"
        price_adj = 0.10
    else:
        strategy = "maintain"
        price_adj = 0.0

    prices = []
    total_suggested = 0
    for i in range(req.days_ahead):
        d = today + timedelta(days=i)
        base_suggested, _, _ = calculate_price(
            req.base_price_paise, req.city, req.property_type, d, current_occ
        )
        # Apply occupancy adjustment
        adjusted = int(base_suggested * (1 + price_adj))
        expected_occ = min(max(current_occ + (price_adj * -1.5), 0), 1.0)

        prices.append({
            "date": d.isoformat(),
            "price_paise": adjusted,
            "expected_occupancy": round(expected_occ * 100, 1),
        })
        total_suggested += adjusted

    avg = total_suggested // req.days_ahead if req.days_ahead > 0 else 0
    est_revenue = int(avg * target_occ * 30)

    return OccupancyPricingResponse(
        listing_id=req.listing_id,
        current_occupancy_percent=req.current_occupancy_percent,
        target_occupancy_percent=req.target_occupancy_percent,
        prices=prices,
        avg_suggested_paise=avg,
        estimated_monthly_revenue_paise=est_revenue,
        strategy=strategy,
    )
