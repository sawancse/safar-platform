from fastapi import FastAPI
from services.scout import router as scout_router
from services.listing_generator import router as generator_router
from services.dynamic_pricing import router as pricing_router
from services.smart_messaging import router as messaging_router
from services.calendar_optimizer import router as calendar_router

app = FastAPI(title="Safar AI Service", version="2.0.0")

app.include_router(scout_router)
app.include_router(generator_router)
app.include_router(pricing_router)
app.include_router(messaging_router)
app.include_router(calendar_router)


@app.get("/health")
def health():
    return {"status": "ok", "service": "ai-service"}
