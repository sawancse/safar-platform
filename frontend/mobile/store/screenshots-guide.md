# Store Screenshots Guide

## Required Screenshots

### Google Play (min 2, max 8 per device type)
- Phone: 1080x1920 or 1440x2560 (16:9 portrait)
- Tablet 7": 1200x1920
- Tablet 10": 1600x2560

### Apple App Store (required sizes)
- iPhone 6.7" (1290x2796) — iPhone 15 Pro Max
- iPhone 6.5" (1284x2778) — iPhone 14 Plus
- iPhone 5.5" (1242x2208) — iPhone 8 Plus
- iPad Pro 12.9" (2048x2732)

## Recommended Screenshot Sequence (8 screens)

1. **Home/Search** — "Find your perfect stay across India"
2. **Search Results** — "28 discovery categories to explore"
3. **Listing Detail** — "Detailed listings with photos & amenities"
4. **Booking Flow** — "Book instantly with secure payments"
5. **Host Dashboard** — "Manage your property effortlessly"
6. **Reviews** — "Trusted reviews from real guests"
7. **Messages** — "Chat directly with hosts"
8. **Map/Nearby** — "Discover stays near you"

## How to Capture

```bash
# Use Expo preview build on a physical device or emulator
eas build --profile preview --platform android
eas build --profile preview --platform ios

# Or use Android emulator with specific resolution
emulator -avd Pixel_7_API_34 -skin 1080x1920

# iOS Simulator
xcrun simctl boot "iPhone 15 Pro Max"
```

## Feature Graphic (Google Play)
- Size: 1024x500
- Design: Safar logo + "Your Journey Starts Here" tagline + India-themed background
- Use brand orange (#f97316)
