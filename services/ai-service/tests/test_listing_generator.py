import pytest
from fastapi.testclient import TestClient
from main import app

client = TestClient(app)


def test_generate_draft_home():
    resp = client.post(
        "/api/v1/ai/listing/generate-draft"
        "?address=123+Marine+Drive&city=Mumbai&property_type=HOME&bedrooms=3"
    )
    assert resp.status_code == 200
    data = resp.json()
    assert "Mumbai" in data["title"] or "Mumbai" in data["description"]
    assert data["suggested_price_paise"] > 0
    assert "wifi" in data["suggested_amenities"]
    assert data["property_type"] == "HOME"


def test_generate_draft_room():
    resp = client.post(
        "/api/v1/ai/listing/generate-draft"
        "?address=45+Linking+Road&city=Mumbai&property_type=ROOM"
    )
    assert resp.status_code == 200
    data = resp.json()
    assert data["property_type"] == "ROOM"
    assert "ac" in data["suggested_amenities"]


def test_generate_draft_commercial():
    resp = client.post(
        "/api/v1/ai/listing/generate-draft"
        "?address=BKC+Office&city=Mumbai&property_type=COMMERCIAL&capacity=20"
    )
    assert resp.status_code == 200
    data = resp.json()
    assert data["property_type"] == "COMMERCIAL"
    assert "projector" in data["suggested_amenities"]


def test_generate_draft_city_specific_price():
    resp_mumbai = client.post(
        "/api/v1/ai/listing/generate-draft?address=A&city=Mumbai&property_type=HOME"
    )
    resp_jaipur = client.post(
        "/api/v1/ai/listing/generate-draft?address=A&city=Jaipur&property_type=HOME"
    )
    assert resp_mumbai.json()["suggested_price_paise"] > resp_jaipur.json()["suggested_price_paise"]


def test_generate_draft_unknown_city_fallback():
    resp = client.post(
        "/api/v1/ai/listing/generate-draft?address=Unknown+St&city=Haridwar&property_type=HOME"
    )
    assert resp.status_code == 200
    assert resp.json()["suggested_price_paise"] == 200000  # default fallback
