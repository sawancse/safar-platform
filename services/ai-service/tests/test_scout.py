import pytest
from fastapi.testclient import TestClient
from main import app

client = TestClient(app)


def test_generate_leads_known_city():
    resp = client.post("/api/v1/ai/scout/generate-leads?city=Goa")
    assert resp.status_code == 200
    data = resp.json()
    assert data["city"] == "Goa"
    assert len(data["leads"]) > 0
    assert data["avg_monthly_income_paise"] > 0


def test_generate_leads_default_limit():
    resp = client.post("/api/v1/ai/scout/generate-leads?city=Mumbai")
    assert resp.status_code == 200
    data = resp.json()
    assert len(data["leads"]) == 5  # default limit capped at 5


def test_generate_leads_includes_hindi_message():
    resp = client.post("/api/v1/ai/scout/generate-leads?city=Jaipur")
    assert resp.status_code == 200
    lead = resp.json()["leads"][0]
    assert "नमस्ते" in lead["outreach_message_hi"]
    assert "Safar" in lead["outreach_message_en"]


def test_generate_leads_unknown_city_uses_defaults():
    resp = client.post("/api/v1/ai/scout/generate-leads?city=Bikaner")
    assert resp.status_code == 200
    data = resp.json()
    assert data["city"] == "Bikaner"
    assert data["avg_monthly_income_paise"] > 0


def test_health():
    resp = client.get("/health")
    assert resp.status_code == 200
    assert resp.json()["status"] == "ok"
