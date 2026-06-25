# Play Store visual assets

## Done
- **feature-graphic.png** — 1024×500, ready to upload (source: `feature-graphic.html`, re-render with Chrome headless if you edit it).

## Screenshots — your turn to capture
Google Play needs **2–8 phone screenshots** (portrait, min 320px shortest side; phone resolution like 1080×2400 is ideal).

### 1. Capture from the installed app
On the phone with the preview APK, open each screen and take a screenshot:

| Save as (`raw/`) | Screen to capture |
|------------------|-------------------|
| `home.png`     | Home / Explore (search + listings) |
| `listing.png`  | A listing detail page |
| `cooks.png`    | Safar Cooks browse or a chef detail |
| `flights.png`  | Flight search or results |
| `services.png` | Services / vendors (cakes, decor, pandit…) |
| `profile.png`  | Profile / My bookings |

Transfer them to this `raw/` folder (USB, Google Photos, email to yourself, etc.). Keep the exact filenames above.

> Tip: capture real content (an actual city search, a real listing) — reviewers and users prefer populated screens over empty states.

### 2. Frame them
```
cd frontend/mobile/store-assets
bash render-screenshots.sh
```
Framed, captioned 1080×1920 images appear in `out/` — upload those to Play Console. Edit the captions in `render-screenshots.sh` (the `SHOTS` array) anytime.

## Other required assets
- **App icon** 512×512 — export from `../assets/icon.png`.
- Optional: tablet screenshots, promo video.
