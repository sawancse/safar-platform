#!/usr/bin/env bash
# Frame raw app screenshots into store-ready 1080x1920 marketing images.
#
# 1. Capture these screens from the installed app and drop the PNGs in ./raw/
#    using EXACTLY these filenames (left column below).
# 2. Run:  bash render-screenshots.sh
# 3. Framed output lands in ./out/  (upload those to Play Console).
#
# Each line: raw-filename|Caption|Subtitle
set -e
DIR="$(cd "$(dirname "$0")" && pwd)"
CHROME="/c/Program Files/Google/Chrome/Application/chrome.exe"
TEMPLATE_WIN="$(cygpath -w "$DIR/frame-template.html" 2>/dev/null || echo "$DIR/frame-template.html")"

# url-encode helper (spaces + reserved chars)
enc() { node -e 'process.stdout.write(encodeURIComponent(process.argv[1]))' "$1"; }

SHOTS=(
  "home.png|Find your perfect stay|Homes, hotels, resorts & unique stays across India"
  "listing.png|Verified listings, real reviews|Photos, ratings & instant booking"
  "cooks.png|Hire home chefs & cooks|Daily meals, parties & special occasions"
  "flights.png|Book flights in seconds|Domestic & international, great fares"
  "services.png|Cakes, decor, pandits & more|Trusted vendors for every occasion"
  "profile.png|Manage trips & bookings|All your stays and services in one place"
)

i=1
for entry in "${SHOTS[@]}"; do
  IFS='|' read -r file caption sub <<< "$entry"
  raw="$DIR/raw/$file"
  if [ ! -f "$raw" ]; then echo "SKIP (missing): raw/$file"; continue; fi
  imgurl="file:///$(cygpath -m "$raw" 2>/dev/null || echo "$raw")"
  qs="img=$(enc "$imgurl")&caption=$(enc "$caption")&sub=$(enc "$sub")"
  out="$DIR/out/$(printf '%02d' $i)-${file}"
  "$CHROME" --headless=new --disable-gpu --hide-scrollbars --force-device-scale-factor=1 \
    --window-size=1080,1920 --screenshot="$(cygpath -w "$out" 2>/dev/null || echo "$out")" \
    "file:///$(cygpath -m "$DIR/frame-template.html")?$qs" >/dev/null 2>&1
  echo "rendered: out/$(printf '%02d' $i)-${file}"
  i=$((i+1))
done
echo "Done. Framed screenshots are in $DIR/out/"
