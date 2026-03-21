from fastapi import APIRouter, Query
from pydantic import BaseModel

router = APIRouter(prefix="/api/v1/ai/scout", tags=["AI Scout"])

HIGH_DEMAND_CITIES: dict[str, dict] = {
    "Goa":       {"avg_occupancy": 0.75, "avg_price_paise": 450000},
    "Manali":    {"avg_occupancy": 0.70, "avg_price_paise": 350000},
    "Udaipur":   {"avg_occupancy": 0.65, "avg_price_paise": 280000},
    "Coorg":     {"avg_occupancy": 0.72, "avg_price_paise": 320000},
    "Jaipur":    {"avg_occupancy": 0.68, "avg_price_paise": 260000},
    "Mumbai":    {"avg_occupancy": 0.80, "avg_price_paise": 600000},
    "Bangalore": {"avg_occupancy": 0.75, "avg_price_paise": 400000},
    "Delhi":     {"avg_occupancy": 0.72, "avg_price_paise": 380000},
}


class ScoutLead(BaseModel):
    address: str
    city: str
    estimated_monthly_income_paise: int
    outreach_message_en: str
    outreach_message_hi: str


class ScoutResponse(BaseModel):
    city: str
    leads: list[ScoutLead]
    avg_monthly_income_paise: int


@router.post("/generate-leads", response_model=ScoutResponse)
def generate_leads(
    city: str = Query(..., description="Target city"),
    property_type: str = Query("HOME", description="Property type"),
    limit: int = Query(5, ge=1, le=20),
) -> ScoutResponse:
    city_data = HIGH_DEMAND_CITIES.get(city, {"avg_occupancy": 0.60, "avg_price_paise": 200000})
    monthly_income = int(city_data["avg_price_paise"] * city_data["avg_occupancy"] * 30)

    leads: list[ScoutLead] = []
    for i in range(min(limit, 5)):
        address = f"Sample Property {i + 1}, {city}"
        income_rupees = monthly_income // 100
        leads.append(
            ScoutLead(
                address=address,
                city=city,
                estimated_monthly_income_paise=monthly_income,
                outreach_message_en=(
                    f"Hi! Your property at {address} could earn "
                    f"₹{income_rupees:,}/month on Safar. "
                    f"List for free — no commission ever."
                ),
                outreach_message_hi=(
                    f"नमस्ते! {address} पर आपकी संपत्ति Safar पर "
                    f"₹{income_rupees:,}/माह कमा सकती है। "
                    f"मुफ्त में लिस्ट करें — कोई कमीशन नहीं।"
                ),
            )
        )

    return ScoutResponse(city=city, leads=leads, avg_monthly_income_paise=monthly_income)
