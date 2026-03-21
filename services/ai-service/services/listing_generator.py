import random
from fastapi import APIRouter, Query
from pydantic import BaseModel

router = APIRouter(prefix="/api/v1/ai/listing", tags=["Listing Generator"])

TITLE_TEMPLATES: dict[str, list[str]] = {
    "HOME": [
        "Cozy {bedrooms}BHK Home in {city}",
        "Spacious {bedrooms}-Bedroom Apartment near {landmark}",
        "Modern {bedrooms}BHK with Stunning City Views in {city}",
        "Charming {bedrooms}BHK Retreat in {city}",
    ],
    "ROOM": [
        "Private Room in Heart of {city}",
        "Comfortable AC Room in {city}",
        "Quiet Room Near {landmark}",
        "Budget-Friendly Room in {city}",
    ],
    "COMMERCIAL": [
        "Professional Meeting Room in {city} — Up to {capacity} People",
        "Modern Event Venue in {city}",
        "Well-Equipped Conference Room in {city}",
        "Flexible Co-Working Space in {city}",
    ],
    "UNIQUE": [
        "One-of-a-Kind Stay in {city}",
        "Unique Experience Retreat near {landmark}",
        "Quirky Hideaway in {city}",
    ],
}

AMENITY_DEFAULTS: dict[str, list[str]] = {
    "HOME":       ["wifi", "ac", "kitchen", "washing_machine", "parking"],
    "ROOM":       ["wifi", "ac", "shared_bathroom"],
    "COMMERCIAL": ["wifi", "projector", "whiteboard", "ac"],
    "UNIQUE":     ["wifi", "ac"],
}

BASE_PRICES: dict[tuple[str, str], int] = {
    ("Mumbai", "HOME"): 500000,    ("Mumbai", "ROOM"): 200000,
    ("Goa", "HOME"): 400000,       ("Goa", "ROOM"): 180000,
    ("Delhi", "HOME"): 350000,     ("Delhi", "ROOM"): 150000,
    ("Bangalore", "HOME"): 300000, ("Bangalore", "ROOM"): 140000,
    ("Chennai", "HOME"): 250000,   ("Hyderabad", "HOME"): 230000,
    ("Jaipur", "HOME"): 200000,    ("Manali", "HOME"): 350000,
}

CITY_LANDMARKS: dict[str, str] = {
    "Mumbai": "Bandra-Worli Sea Link",
    "Goa": "Calangute Beach",
    "Delhi": "Connaught Place",
    "Bangalore": "MG Road",
    "Chennai": "Marina Beach",
    "Jaipur": "Hawa Mahal",
    "Manali": "Rohtang Pass",
    "Udaipur": "Lake Pichola",
}


class GeneratedDraft(BaseModel):
    title: str
    description: str
    suggested_amenities: list[str]
    suggested_price_paise: int
    city: str
    property_type: str


@router.post("/generate-draft", response_model=GeneratedDraft)
def generate_draft(
    address: str = Query(..., description="Property address"),
    city: str = Query(..., description="City"),
    property_type: str = Query("HOME", description="HOME | ROOM | COMMERCIAL | UNIQUE"),
    bedrooms: int = Query(2, ge=1, le=10),
    capacity: int = Query(10, ge=1, le=200),
) -> GeneratedDraft:
    templates = TITLE_TEMPLATES.get(property_type, TITLE_TEMPLATES["HOME"])
    landmark = CITY_LANDMARKS.get(city, f"{city} City Centre")

    title = random.choice(templates).format(
        bedrooms=bedrooms, city=city, landmark=landmark, capacity=capacity
    )

    travel_type = "families and groups" if bedrooms > 2 else "solo travelers and couples"
    description = (
        f"Welcome to this wonderful {property_type.lower().replace('_', ' ')} in {city}. "
        f"Located at {address}, this property offers comfortable accommodation "
        f"with modern amenities. Perfect for {travel_type}. "
        f"Enjoy easy access to local attractions, restaurants, and transport. "
        f"Book now and experience the best of {city}!"
    )

    price = BASE_PRICES.get((city, property_type), 200000)
    amenities = AMENITY_DEFAULTS.get(property_type, ["wifi"])

    return GeneratedDraft(
        title=title,
        description=description,
        suggested_amenities=amenities,
        suggested_price_paise=price,
        city=city,
        property_type=property_type,
    )
