#!/bin/bash
#
# Launches all Safar backend services in a SINGLE Windows Terminal window,
# one tab per service. Requires Windows Terminal (wt.exe).
#
# Usage:  ./startAllServicesTabs.sh
#

BASE_WIN="C:\\Users\\Win-10\\safar-platform"
HELPER="$BASE_WIN\\_runService.sh"
GIT_BASH="C:\\Program Files\\Git\\bin\\bash.exe"

# Java/Spring Boot services (one tab each)
SERVICES=(
  "api-gateway"
  "auth-service"
  "user-service"
  "listing-service"
  "search-service"
  "booking-service"
  "payment-service"
  "review-service"
  "media-service"
  "notification-service"
  "messaging-service"
  "chef-service"
)

# Build wt.exe arguments. The command passed to bash MUST NOT contain ';'
# because wt parses ';' as a tab separator regardless of quoting.
WT_ARGS=()
FIRST=1
for svc in "${SERVICES[@]}"; do
  UNIX_DIR="/c/Users/Win-10/safar-platform/services/$svc"
  if [ ! -d "$UNIX_DIR" ]; then
    echo "WARN: $UNIX_DIR not found - skipping"
    continue
  fi

  if [ $FIRST -eq 1 ]; then
    FIRST=0
  else
    WT_ARGS+=(";")
  fi

  WT_ARGS+=(
    "new-tab"
    "--title" "$svc"
    "-d" "$BASE_WIN"
    "$GIT_BASH" "-c" "./_runService.sh $svc"
  )
done

echo "Launching ${#SERVICES[@]} services in Windows Terminal tabs..."
wt.exe "${WT_ARGS[@]}"
