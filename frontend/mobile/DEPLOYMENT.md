# Safar Mobile App - Deployment Guide

## Prerequisites

### Accounts Required
1. **Expo Account**: https://expo.dev (already set up, project ID: `5280ee28-a8b1-45c8-b64e-e2cba6666a17`)
2. **Google Play Console**: https://play.google.com/console ($25 one-time fee)
3. **Apple Developer Account**: https://developer.apple.com ($99/year)

### Tools
- EAS CLI: `npm install -g eas-cli` (already installed v18.4.0)
- Node.js 18+

---

## Step-by-Step Deployment

### 1. Login to EAS
```bash
eas login
```

### 2. Google Play Setup

#### a. Create App in Play Console
1. Go to Play Console → Create App
2. App name: "Safar - Property Rentals India"
3. Default language: English (India)
4. App type: App
5. Category: Travel & Local
6. Free

#### b. Create Service Account for Automated Uploads
1. Go to Play Console → Setup → API access
2. Link to Google Cloud Project
3. Create Service Account with "Service Account User" role
4. Grant "Release Manager" permission in Play Console
5. Download JSON key → save as `frontend/mobile/google-service-account.json`
6. **Add to .gitignore** (already handled)

#### c. Fill Data Safety Form
Use `store/google-play/data-safety.json` as reference.

#### d. Upload Store Listing
Use `store/google-play/listing.json` for descriptions.

### 3. Apple App Store Setup

#### a. Create App in App Store Connect
1. Go to https://appstoreconnect.apple.com
2. My Apps → + → New App
3. Bundle ID: `com.ysafar.app`
4. Name: "Safar - Property Rentals India"

#### b. Update eas.json with Apple Credentials
Edit `eas.json` → submit → production → ios:
```json
{
  "appleId": "your-real-apple-id@example.com",
  "ascAppId": "1234567890",
  "appleTeamId": "ABCDE12345"
}
```

### 4. Privacy Policy
Deploy `store/privacy-policy.html` to https://ysafar.com/privacy-policy
```bash
# Upload to S3 (same bucket as admin dashboard)
aws s3 cp store/privacy-policy.html s3://safar-admin-dashboard/privacy-policy.html --content-type text/html
```

### 5. Build for Production

#### Preview Build (test on device first)
```bash
# Android APK for internal testing
npm run build:preview:android

# iOS for internal testing (requires Apple Developer account)
npm run build:preview:ios
```

#### Production Build
```bash
# Android AAB (for Play Store)
npm run build:prod:android

# iOS (for App Store)
npm run build:prod:ios

# Both platforms
npm run build:prod:all
```

### 6. Submit to Stores

#### Android (Google Play)
```bash
# Submits to internal testing track
npm run submit:android
```
Then in Play Console:
1. Internal testing → Review & roll out
2. Closed testing → Add testers → Review & roll out
3. Production → Review & roll out (after testing)

#### iOS (App Store)
```bash
npm run submit:ios
```
Then in App Store Connect:
1. Select build under "Prepare for Submission"
2. Fill in app review info
3. Submit for Review (usually 24-48 hours)

### 7. Post-Launch: OTA Updates
For JS-only changes (no native module changes):
```bash
npm run update "Fix: description of update"
```

---

## Build Commands Reference

| Command | Description |
|---------|-------------|
| `npm run build:preview:android` | APK for internal testing |
| `npm run build:preview:ios` | iOS build for internal testing |
| `npm run build:prod:android` | AAB for Google Play |
| `npm run build:prod:ios` | IPA for App Store |
| `npm run build:prod:all` | Both platforms |
| `npm run submit:android` | Upload to Play Console |
| `npm run submit:ios` | Upload to App Store Connect |
| `npm run update "msg"` | OTA update (JS changes only) |

---

## Checklist Before First Submit

- [ ] Create Google Play Console account ($25)
- [ ] Create Apple Developer account ($99/year)
- [ ] Generate Google service account key → `google-service-account.json`
- [ ] Update `eas.json` with Apple credentials (appleId, ascAppId, appleTeamId)
- [ ] Deploy privacy policy to https://ysafar.com/privacy-policy
- [ ] Create Google Play store listing with screenshots
- [ ] Create App Store Connect listing with screenshots
- [ ] Fill Google Play data safety form
- [ ] Fill App Store privacy nutrition labels
- [ ] Run preview build and test on physical devices
- [ ] Run production build
- [ ] Submit to both stores
- [ ] Set up Google Firebase for `google-services.json` (push notifications on Android)

---

## Important Files

| File | Purpose |
|------|---------|
| `app.json` | Expo config (version, permissions, plugins) |
| `eas.json` | Build & submit profiles |
| `google-service-account.json` | Play Store upload key (DO NOT commit) |
| `google-services.json` | Firebase config for Android (DO NOT commit) |
| `store/` | Store listing metadata and privacy policy |
