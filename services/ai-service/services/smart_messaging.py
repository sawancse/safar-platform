"""
Smart Messaging Service for Safar Platform.
Context-aware message templates, reply suggestions, and auto-responses.
Bilingual: English + Hindi.
"""

from datetime import datetime
from enum import Enum
from typing import Optional

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel

router = APIRouter(prefix="/api/v1/ai/messaging", tags=["Smart Messaging"])


# ──────────────────────────────────────────────
# Templates
# ──────────────────────────────────────────────
TEMPLATES: dict[str, dict[str, str]] = {
    "booking_confirmation": {
        "en": "Hi {guest_name}! Your booking at {listing_name} is confirmed for {check_in} to {check_out}. Total: ₹{amount}. We're excited to host you! 🏠",
        "hi": "नमस्ते {guest_name}! {listing_name} पर आपकी बुकिंग {check_in} से {check_out} तक कन्फर्म है। कुल: ₹{amount}। हम आपकी मेज़बानी के लिए उत्साहित हैं! 🏠",
    },
    "check_in_reminder": {
        "en": "Reminder: Your check-in at {listing_name} is tomorrow ({check_in}). Check-in time: {checkin_time}. Address: {address}. Need directions? Just ask!",
        "hi": "रिमाइंडर: {listing_name} पर आपका चेक-इन कल ({check_in}) है। चेक-इन समय: {checkin_time}। पता: {address}। रास्ता चाहिए? बस पूछें!",
    },
    "review_request": {
        "en": "Hi {guest_name}! Hope you enjoyed your stay at {listing_name}. Would you mind leaving a review? It helps other travelers and means a lot to us! ⭐",
        "hi": "नमस्ते {guest_name}! उम्मीद है {listing_name} पर आपका अनुभव अच्छा रहा। क्या आप एक रिव्यू दे सकते हैं? इससे अन्य यात्रियों को मदद मिलती है! ⭐",
    },
    "price_alert": {
        "en": "Great news! The price for {listing_name} on {dates} has dropped to ₹{amount}/night. Book now before it goes up!",
        "hi": "बढ़िया खबर! {listing_name} की कीमत {dates} के लिए ₹{amount}/रात हो गई है। बढ़ने से पहले बुक करें!",
    },
    "special_offer": {
        "en": "🎉 Special offer! Get {discount}% off at {listing_name} for stays between {start_date} and {end_date}. Limited availability!",
        "hi": "🎉 स्पेशल ऑफर! {listing_name} पर {start_date} से {end_date} तक {discount}% छूट। सीमित उपलब्धता!",
    },
    "payment_reminder": {
        "en": "Hi {guest_name}, your payment of ₹{amount} for {listing_name} is pending. Please complete the payment to confirm your booking.",
        "hi": "नमस्ते {guest_name}, {listing_name} के लिए ₹{amount} का भुगतान बाकी है। बुकिंग कन्फर्म करने के लिए कृपया भुगतान पूरा करें।",
    },
    "welcome_host": {
        "en": "Welcome to Safar, {host_name}! Your listing {listing_name} is now live. Here are some tips to get your first booking...",
        "hi": "सफ़र में आपका स्वागत है, {host_name}! आपकी लिस्टिंग {listing_name} अब लाइव है। पहली बुकिंग पाने के लिए कुछ सुझाव...",
    },
}

# Common query patterns → auto-response
AUTO_RESPONSES: dict[str, dict[str, str]] = {
    "directions": {
        "keywords": ["direction", "how to reach", "kaise aaye", "address", "location", "map", "rasta", "route"],
        "en": "To reach {listing_name}: {address}. From {nearest_landmark}: {directions}. I'll share the Google Maps link: {maps_link}",
        "hi": "{listing_name} पहुँचने के लिए: {address}। {nearest_landmark} से: {directions}। Google Maps लिंक: {maps_link}",
    },
    "checkin_time": {
        "keywords": ["check-in time", "check in time", "checkin", "what time", "kitne baje", "kab aaye"],
        "en": "Check-in time is {checkin_time}. Early check-in available on request (subject to availability). Check-out by {checkout_time}.",
        "hi": "चेक-इन का समय {checkin_time} है। अनुरोध पर अर्ली चेक-इन उपलब्ध (उपलब्धता के अनुसार)। चेक-आउट {checkout_time} तक।",
    },
    "wifi": {
        "keywords": ["wifi", "wi-fi", "internet", "password", "net"],
        "en": "WiFi network: {wifi_name}. Password: {wifi_password}. Speed: {wifi_speed}. If you face any issues, please restart the router near the entrance.",
        "hi": "WiFi नेटवर्क: {wifi_name}। पासवर्ड: {wifi_password}। स्पीड: {wifi_speed}। कोई समस्या हो तो प्रवेश द्वार के पास राउटर रीस्टार्ट करें।",
    },
    "parking": {
        "keywords": ["parking", "car", "bike", "vehicle", "gaadi"],
        "en": "Parking is available at the property. {parking_details}. Please park in the designated area only.",
        "hi": "प्रॉपर्टी पर पार्किंग उपलब्ध है। {parking_details}। कृपया निर्धारित क्षेत्र में ही पार्क करें।",
    },
    "cancellation": {
        "keywords": ["cancel", "cancellation", "refund", "cancel karna", "vapas"],
        "en": "Our cancellation policy: {cancellation_policy}. For cancellation requests, please go to My Bookings > Cancel. Refunds are processed within 5-7 business days.",
        "hi": "हमारी कैंसिलेशन पॉलिसी: {cancellation_policy}। कैंसिल करने के लिए, My Bookings > Cancel पर जाएं। रिफंड 5-7 कार्य दिवसों में प्रोसेस होता है।",
    },
    "food": {
        "keywords": ["food", "breakfast", "lunch", "dinner", "restaurant", "khana", "nashta", "meal"],
        "en": "Nearby food options: {food_options}. {meals_info}. We recommend trying the local {local_specialty}!",
        "hi": "आस-पास खाने के विकल्प: {food_options}। {meals_info}। हम स्थानीय {local_specialty} ज़रूर ट्राई करने की सलाह देते हैं!",
    },
    "amenities": {
        "keywords": ["amenities", "facility", "facilities", "towel", "soap", "suvidha", "ac", "geyser"],
        "en": "Available amenities: {amenities_list}. If you need anything else, please let us know and we'll do our best to arrange it.",
        "hi": "उपलब्ध सुविधाएं: {amenities_list}। अगर आपको कुछ और चाहिए, तो कृपया बताएं और हम व्यवस्था करने की पूरी कोशिश करेंगे।",
    },
}


# ──────────────────────────────────────────────
# Models
# ──────────────────────────────────────────────
class TemplateRequest(BaseModel):
    template_type: str  # booking_confirmation, check_in_reminder, etc.
    language: str = "en"  # en or hi
    params: dict[str, str] = {}  # {guest_name, listing_name, check_in, etc.}


class TemplateResponse(BaseModel):
    template_type: str
    language: str
    message: str
    raw_template: str


class ReplySuggestRequest(BaseModel):
    messages: list[dict[str, str]]  # [{role: "guest"/"host", content: "..."}]
    listing_context: dict[str, str] = {}  # {listing_name, city, property_type}
    language: str = "en"


class ReplySuggestion(BaseModel):
    replies: list[str]
    detected_intent: str
    confidence: float


class AutoResponseRequest(BaseModel):
    query: str
    listing_context: dict[str, str] = {}
    language: str = "en"


class AutoResponseResult(BaseModel):
    response: str
    topic: str
    needs_personalization: bool
    placeholders: list[str]


# ──────────────────────────────────────────────
# Intent Detection
# ──────────────────────────────────────────────
def _detect_intent(text: str) -> tuple[str, float]:
    text_lower = text.lower()
    for topic, data in AUTO_RESPONSES.items():
        for kw in data["keywords"]:
            if kw in text_lower:
                return topic, 0.85
    # Fallback intents
    if any(w in text_lower for w in ["thank", "thanks", "dhanyavad", "shukriya"]):
        return "gratitude", 0.90
    if any(w in text_lower for w in ["problem", "issue", "complaint", "dikkat", "pareshani"]):
        return "complaint", 0.80
    if any(w in text_lower for w in ["book", "available", "reserve", "khali"]):
        return "booking_inquiry", 0.75
    if any(w in text_lower for w in ["price", "cost", "rate", "kitna", "kharcha"]):
        return "pricing_inquiry", 0.75
    return "general", 0.50


def _generate_replies(intent: str, language: str, context: dict) -> list[str]:
    listing = context.get("listing_name", "our property")

    replies_map: dict[str, dict[str, list[str]]] = {
        "directions": {
            "en": [
                f"I'll share the exact location pin for {listing} right away!",
                f"Here are the directions to {listing}. Let me know if you need more help.",
                "I'll send you the Google Maps link. It's easy to find!",
            ],
            "hi": [
                f"मैं {listing} की सटीक लोकेशन अभी भेजता हूँ!",
                f"{listing} तक पहुँचने का रास्ता बताता हूँ। और मदद चाहिए तो बताइए।",
                "Google Maps लिंक भेज रहा हूँ। ढूंढना आसान है!",
            ],
        },
        "checkin_time": {
            "en": [
                "Check-in is from 2 PM. Would you like early check-in?",
                "You can check in anytime after 2 PM. I'll be available to welcome you.",
                "Check-in: 2 PM, Check-out: 11 AM. Need flexibility? Let me know!",
            ],
            "hi": [
                "चेक-इन दोपहर 2 बजे से है। क्या अर्ली चेक-इन चाहिए?",
                "आप 2 बजे के बाद कभी भी आ सकते हैं। मैं स्वागत के लिए उपलब्ध रहूँगा।",
                "चेक-इन: 2 PM, चेक-आउट: 11 AM। फ्लेक्सिबिलिटी चाहिए? बताइए!",
            ],
        },
        "wifi": {
            "en": [
                "WiFi details are on the card near the entrance. Let me share them here too.",
                "I'll send the WiFi password right away!",
                "WiFi is available throughout the property. Here are the details...",
            ],
            "hi": [
                "WiFi की जानकारी प्रवेश द्वार पर कार्ड पर है। यहाँ भी भेजता हूँ।",
                "WiFi पासवर्ड अभी भेजता हूँ!",
                "पूरी प्रॉपर्टी में WiFi उपलब्ध है। ये रहीं डिटेल्स...",
            ],
        },
        "gratitude": {
            "en": [
                "You're welcome! Let me know if you need anything else.",
                "Happy to help! Enjoy your stay!",
                "My pleasure! Don't hesitate to reach out anytime.",
            ],
            "hi": [
                "आपका स्वागत है! कुछ और चाहिए तो बताइए।",
                "मदद करके खुशी हुई! अपने स्टे का आनंद लें!",
                "मेरी खुशी! कभी भी संपर्क करने में संकोच न करें।",
            ],
        },
        "complaint": {
            "en": [
                "I'm sorry to hear that. Let me look into this right away.",
                "I apologize for the inconvenience. Can you share more details?",
                "Thank you for letting me know. I'll fix this immediately.",
            ],
            "hi": [
                "यह सुनकर दुख हुआ। मैं तुरंत इसे देखता हूँ।",
                "असुविधा के लिए माफी। क्या आप और बता सकते हैं?",
                "बताने के लिए धन्यवाद। मैं तुरंत ठीक करता हूँ।",
            ],
        },
        "booking_inquiry": {
            "en": [
                f"Yes, {listing} is available for your dates! Shall I help you book?",
                "Let me check availability for you. What dates are you looking at?",
                "I'd love to host you! The property is available. Want to proceed?",
            ],
            "hi": [
                f"हाँ, {listing} आपकी तारीखों के लिए उपलब्ध है! बुकिंग में मदद करूँ?",
                "आपके लिए उपलब्धता चेक करता हूँ। कौन सी तारीखें हैं?",
                "आपकी मेज़बानी करके खुशी होगी! प्रॉपर्टी उपलब्ध है। आगे बढ़ें?",
            ],
        },
        "pricing_inquiry": {
            "en": [
                f"The rate for {listing} starts from the listed price. Any specific dates?",
                "I can offer a special discount for longer stays. Interested?",
                "Let me check the best rate for your dates.",
            ],
            "hi": [
                f"{listing} की दर लिस्टेड प्राइस से शुरू है। कोई विशेष तारीखें?",
                "लंबे स्टे पर स्पेशल डिस्काउंट दे सकता हूँ। रुचि है?",
                "आपकी तारीखों के लिए बेस्ट रेट चेक करता हूँ।",
            ],
        },
        "general": {
            "en": [
                "Thanks for your message! How can I help you?",
                "I'd be happy to help. Could you tell me more?",
                "Sure, let me assist you with that.",
            ],
            "hi": [
                "आपके मैसेज के लिए धन्यवाद! मैं कैसे मदद कर सकता हूँ?",
                "मदद करके खुशी होगी। क्या आप और बता सकते हैं?",
                "ज़रूर, मैं इसमें आपकी मदद करता हूँ।",
            ],
        },
    }

    lang_replies = replies_map.get(intent, replies_map["general"])
    return lang_replies.get(language, lang_replies["en"])


# ──────────────────────────────────────────────
# Endpoints
# ──────────────────────────────────────────────
@router.post("/template", response_model=TemplateResponse)
async def generate_template(request: TemplateRequest):
    template_data = TEMPLATES.get(request.template_type)
    if not template_data:
        raise HTTPException(404, f"Template '{request.template_type}' not found. Available: {list(TEMPLATES.keys())}")

    raw = template_data.get(request.language, template_data["en"])
    try:
        message = raw.format(**request.params)
    except KeyError:
        message = raw  # Return raw if params missing

    return TemplateResponse(
        template_type=request.template_type,
        language=request.language,
        message=message,
        raw_template=raw,
    )


@router.post("/reply-suggest", response_model=ReplySuggestion)
async def reply_suggest(request: ReplySuggestRequest):
    if not request.messages:
        raise HTTPException(400, "At least one message required")

    last_guest_msg = ""
    for msg in reversed(request.messages):
        if msg.get("role") == "guest":
            last_guest_msg = msg.get("content", "")
            break

    if not last_guest_msg:
        last_guest_msg = request.messages[-1].get("content", "")

    intent, confidence = _detect_intent(last_guest_msg)
    replies = _generate_replies(intent, request.language, request.listing_context)

    return ReplySuggestion(
        replies=replies,
        detected_intent=intent,
        confidence=confidence,
    )


@router.post("/auto-response", response_model=AutoResponseResult)
async def auto_response(request: AutoResponseRequest):
    intent, confidence = _detect_intent(request.query)

    if intent in AUTO_RESPONSES:
        data = AUTO_RESPONSES[intent]
        template = data.get(request.language, data["en"])
        # Find placeholders
        import re
        placeholders = re.findall(r"\{(\w+)\}", template)
        # Fill what we can from context
        try:
            response = template.format(**{
                **{p: f"[{p}]" for p in placeholders},
                **request.listing_context,
            })
        except (KeyError, IndexError):
            response = template

        return AutoResponseResult(
            response=response,
            topic=intent,
            needs_personalization=len(placeholders) > len(request.listing_context),
            placeholders=placeholders,
        )

    # Fallback
    fallback = {
        "en": "Thank you for your message. Let me get back to you shortly.",
        "hi": "आपके मैसेज के लिए धन्यवाद। मैं जल्दी ही जवाब दूंगा।",
    }
    return AutoResponseResult(
        response=fallback.get(request.language, fallback["en"]),
        topic="general",
        needs_personalization=False,
        placeholders=[],
    )
