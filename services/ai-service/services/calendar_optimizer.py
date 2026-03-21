"""
Calendar Optimization Service for Safar Platform.
Analyzes host calendar patterns and suggests optimizations for revenue maximization.
"""

from datetime import date, timedelta
from typing import Optional

from fastapi import APIRouter, Query, HTTPException
from pydantic import BaseModel, Field

router = APIRouter(prefix="/api/v1/ai/calendar", tags=["Calendar Optimizer"])


# ──────────────────────────────────────────────
# Models
# ──────────────────────────────────────────────
class BookingSlot(BaseModel):
    check_in: str
    check_out: str
    price_paise: int = 0


class GapRecommendation(BaseModel):
    gap_start: str
    gap_end: str
    gap_nights: int
    preceding_checkout: Optional[str] = None
    following_checkin: Optional[str] = None
    recommended_discount_percent: float
    recommended_price_paise: int
    reason: str


class MinStayRecommendation(BaseModel):
    period_start: str
    period_end: str
    current_min_stay: int
    recommended_min_stay: int
    reason: str


class MaintenanceWindow(BaseModel):
    suggested_start: str
    suggested_end: str
    duration_days: int
    reason: str


class RevenueProjection(BaseModel):
    current_revenue_paise: int
    optimized_revenue_paise: int
    increase_paise: int
    increase_percent: float


class OptimizationResult(BaseModel):
    listing_id: str
    analysis_period_days: int
    current_occupancy_percent: float
    projected_occupancy_percent: float
    gap_recommendations: list[GapRecommendation]
    min_stay_recommendations: list[MinStayRecommendation]
    maintenance_windows: list[MaintenanceWindow]
    revenue_projection: RevenueProjection


class DayInsight(BaseModel):
    day_of_week: str
    booking_frequency: float  # 0-1
    avg_price_paise: int
    avg_lead_time_days: int


class MonthlyInsight(BaseModel):
    month: str  # "January", "February", etc.
    occupancy_percent: float
    avg_price_paise: int
    total_revenue_paise: int
    booking_count: int


class CalendarInsights(BaseModel):
    listing_id: str
    period: str
    overall_occupancy_percent: float
    avg_booking_lead_time_days: int
    avg_stay_duration_nights: float
    rev_pan_paise: int  # Revenue Per Available Night
    day_insights: list[DayInsight]
    monthly_insights: list[MonthlyInsight]
    suggested_actions: list[str]


class OptimizeRequest(BaseModel):
    listing_id: str
    base_price_paise: int
    city: str = "Mumbai"
    property_type: str = "HOME"
    bookings: list[BookingSlot] = []  # Existing bookings
    analysis_days: int = 60
    current_min_stay: int = 1


# ──────────────────────────────────────────────
# Core Analysis Logic
# ──────────────────────────────────────────────
def _find_gaps(bookings: list[BookingSlot], start: date, end: date) -> list[tuple[date, date]]:
    if not bookings:
        return [(start, end)]

    sorted_bookings = sorted(bookings, key=lambda b: b.check_in)
    gaps = []

    # Gap before first booking
    first_checkin = date.fromisoformat(sorted_bookings[0].check_in)
    if first_checkin > start:
        gaps.append((start, first_checkin - timedelta(days=1)))

    # Gaps between bookings
    for i in range(len(sorted_bookings) - 1):
        curr_checkout = date.fromisoformat(sorted_bookings[i].check_out)
        next_checkin = date.fromisoformat(sorted_bookings[i + 1].check_in)
        if (next_checkin - curr_checkout).days > 0:
            gaps.append((curr_checkout, next_checkin - timedelta(days=1)))

    # Gap after last booking
    last_checkout = date.fromisoformat(sorted_bookings[-1].check_out)
    if last_checkout < end:
        gaps.append((last_checkout, end))

    return gaps


def _calculate_gap_discount(gap_nights: int, is_between_bookings: bool) -> float:
    if not is_between_bookings:
        return 5.0  # Small discount for open-ended gaps
    if gap_nights == 1:
        return 25.0  # Aggressive discount for single-night gaps
    elif gap_nights == 2:
        return 20.0
    elif gap_nights <= 4:
        return 15.0
    else:
        return 10.0


def _suggest_min_stay(
    bookings: list[BookingSlot], month: int, current_min: int
) -> Optional[tuple[int, str]]:
    # Peak months → higher min stay
    peak_months = {10, 11, 12, 1, 3, 4, 5}
    off_months = {6, 7, 8, 9, 2}

    if month in peak_months and current_min < 2:
        return 2, "Peak season — increasing minimum stay captures higher-value bookings"
    elif month in off_months and current_min > 1:
        return 1, "Off-season — reducing minimum stay increases booking chances"
    return None


def _find_maintenance_windows(
    bookings: list[BookingSlot], start: date, end: date
) -> list[MaintenanceWindow]:
    gaps = _find_gaps(bookings, start, end)
    windows = []

    for gap_start, gap_end in gaps:
        gap_days = (gap_end - gap_start).days + 1
        # Only suggest maintenance for gaps of 3+ days in low-demand periods
        if gap_days >= 3 and gap_start.month in {6, 7, 8, 9, 2}:
            maint_days = min(gap_days, 5)
            windows.append(MaintenanceWindow(
                suggested_start=gap_start.isoformat(),
                suggested_end=(gap_start + timedelta(days=maint_days - 1)).isoformat(),
                duration_days=maint_days,
                reason=f"Low-demand period — good time for maintenance/renovation",
            ))

    return windows[:3]  # Max 3 suggestions


# ──────────────────────────────────────────────
# Endpoints
# ──────────────────────────────────────────────
@router.post("/optimize", response_model=OptimizationResult)
async def optimize_calendar(request: OptimizeRequest):
    today = date.today()
    end_date = today + timedelta(days=request.analysis_days)

    # Find gaps
    gaps = _find_gaps(request.bookings, today, end_date)
    gap_recs: list[GapRecommendation] = []

    for i, (gap_start, gap_end) in enumerate(gaps):
        gap_nights = (gap_end - gap_start).days + 1
        is_between = i > 0 or (request.bookings and date.fromisoformat(request.bookings[0].check_in) > gap_end)
        discount = _calculate_gap_discount(gap_nights, is_between)
        rec_price = int(request.base_price_paise * (1 - discount / 100))

        # Find surrounding bookings
        preceding = None
        following = None
        sorted_bookings = sorted(request.bookings, key=lambda b: b.check_in)
        for b in sorted_bookings:
            co = date.fromisoformat(b.check_out)
            ci = date.fromisoformat(b.check_in)
            if co <= gap_start:
                preceding = b.check_out
            if ci >= gap_end and following is None:
                following = b.check_in

        if gap_nights <= 7:  # Only recommend for short gaps
            gap_recs.append(GapRecommendation(
                gap_start=gap_start.isoformat(),
                gap_end=gap_end.isoformat(),
                gap_nights=gap_nights,
                preceding_checkout=preceding,
                following_checkin=following,
                recommended_discount_percent=discount,
                recommended_price_paise=rec_price,
                reason=f"{gap_nights}-night gap — {discount:.0f}% discount fills it and avoids idle revenue loss",
            ))

    # Min stay recommendations
    min_stay_recs: list[MinStayRecommendation] = []
    for month_offset in range(0, request.analysis_days // 30 + 1):
        check_month = (today.month + month_offset - 1) % 12 + 1
        check_year = today.year + (today.month + month_offset - 1) // 12
        result = _suggest_min_stay(request.bookings, check_month, request.current_min_stay)
        if result:
            rec_min, reason = result
            period_start = date(check_year, check_month, 1)
            if check_month == 12:
                period_end = date(check_year + 1, 1, 1) - timedelta(days=1)
            else:
                period_end = date(check_year, check_month + 1, 1) - timedelta(days=1)
            min_stay_recs.append(MinStayRecommendation(
                period_start=period_start.isoformat(),
                period_end=period_end.isoformat(),
                current_min_stay=request.current_min_stay,
                recommended_min_stay=rec_min,
                reason=reason,
            ))

    # Maintenance windows
    maint_windows = _find_maintenance_windows(request.bookings, today, end_date)

    # Revenue projection
    booked_nights = sum(
        (date.fromisoformat(b.check_out) - date.fromisoformat(b.check_in)).days
        for b in request.bookings
        if date.fromisoformat(b.check_in) >= today
    )
    total_nights = request.analysis_days
    current_occ = (booked_nights / total_nights * 100) if total_nights > 0 else 0
    current_rev = sum(
        b.price_paise or request.base_price_paise
        for b in request.bookings
        if date.fromisoformat(b.check_in) >= today
    )

    # Projected: fill gaps with discounted rates
    gap_fill_rev = sum(r.recommended_price_paise * r.gap_nights for r in gap_recs)
    gap_fill_nights = sum(r.gap_nights for r in gap_recs)
    optimized_rev = current_rev + gap_fill_rev
    projected_occ = min(100, current_occ + (gap_fill_nights / total_nights * 100))

    increase = optimized_rev - current_rev
    increase_pct = (increase / current_rev * 100) if current_rev > 0 else 0

    return OptimizationResult(
        listing_id=request.listing_id,
        analysis_period_days=request.analysis_days,
        current_occupancy_percent=round(current_occ, 1),
        projected_occupancy_percent=round(projected_occ, 1),
        gap_recommendations=gap_recs,
        min_stay_recommendations=min_stay_recs,
        maintenance_windows=maint_windows,
        revenue_projection=RevenueProjection(
            current_revenue_paise=current_rev,
            optimized_revenue_paise=optimized_rev,
            increase_paise=increase,
            increase_percent=round(increase_pct, 1),
        ),
    )


@router.get("/insights/{listing_id}", response_model=CalendarInsights)
async def calendar_insights(
    listing_id: str,
    base_price_paise: int = Query(300000, ge=100),
    city: str = Query("Mumbai"),
    total_bookings: int = Query(15, ge=0, description="Total bookings in last 90 days"),
    avg_stay_nights: float = Query(2.5, ge=1),
    occupancy_percent: float = Query(65, ge=0, le=100),
):
    # Generate day-of-week insights
    day_names = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]
    # Weekend bookings typically higher
    freq_pattern = [0.4, 0.35, 0.45, 0.55, 0.75, 0.90, 0.85]
    price_pattern = [0.90, 0.90, 0.95, 1.0, 1.10, 1.20, 1.15]
    lead_pattern = [14, 12, 10, 8, 5, 3, 4]

    day_insights = [
        DayInsight(
            day_of_week=day_names[i],
            booking_frequency=round(freq_pattern[i], 2),
            avg_price_paise=int(base_price_paise * price_pattern[i]),
            avg_lead_time_days=lead_pattern[i],
        )
        for i in range(7)
    ]

    # Monthly insights (last 3 months + next 3)
    today = date.today()
    month_names = [
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    ]
    monthly = []
    for offset in range(-2, 4):
        m = (today.month + offset - 1) % 12 + 1
        y = today.year + (today.month + offset - 1) // 12
        # Seasonal adjustment
        seasonal_occ = occupancy_percent
        if m in {10, 11, 12, 1}: seasonal_occ *= 1.2
        elif m in {6, 7, 8}: seasonal_occ *= 0.8
        seasonal_occ = min(100, seasonal_occ)

        bcount = max(1, int(total_bookings / 3 * (seasonal_occ / occupancy_percent)))
        rev = int(base_price_paise * bcount * avg_stay_nights)

        monthly.append(MonthlyInsight(
            month=f"{month_names[m - 1]} {y}",
            occupancy_percent=round(seasonal_occ, 1),
            avg_price_paise=base_price_paise,
            total_revenue_paise=rev,
            booking_count=bcount,
        ))

    # RevPAN
    booked_nights = int(90 * occupancy_percent / 100)
    total_rev = base_price_paise * booked_nights
    rev_pan = total_rev // 90 if booked_nights > 0 else 0

    # Suggested actions
    actions = []
    if occupancy_percent < 50:
        actions.append("Consider lowering base price by 10-15% to increase bookings")
        actions.append("Enable Instant Book to capture impulse travelers")
    if occupancy_percent > 80:
        actions.append("Increase weekend prices by 20-25% — demand supports it")
        actions.append("Consider raising minimum stay to 2 nights during peak")
    if avg_stay_nights < 2:
        actions.append("Offer a 10% discount for 3+ night stays to increase avg stay duration")
    if total_bookings < 10:
        actions.append("Add more photos and update description to improve conversion")
        actions.append("Enable flexible cancellation to attract first-time guests")
    actions.append(f"Your RevPAN (₹{rev_pan // 100}/night) — aim for 10% improvement next quarter")

    avg_lead = int(sum(lead_pattern) / len(lead_pattern))

    return CalendarInsights(
        listing_id=listing_id,
        period="Last 90 days",
        overall_occupancy_percent=round(occupancy_percent, 1),
        avg_booking_lead_time_days=avg_lead,
        avg_stay_duration_nights=round(avg_stay_nights, 1),
        rev_pan_paise=rev_pan,
        day_insights=day_insights,
        monthly_insights=monthly,
        suggested_actions=actions,
    )
