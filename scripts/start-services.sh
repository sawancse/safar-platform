#!/bin/bash
# Safar Platform - Service Starter with JVM Memory Governance
# Usage: ./scripts/start-services.sh [service-name]
# Run all: ./scripts/start-services.sh all

set -e

SAFAR_HOME="$(cd "$(dirname "$0")/.." && pwd)"

# JVM memory settings per service (tuned for dev machine with 8-16GB RAM)
declare -A HEAP_SETTINGS=(
  ["api-gateway"]="-Xms64m -Xmx192m"
  ["auth-service"]="-Xms64m -Xmx256m"
  ["user-service"]="-Xms64m -Xmx256m"
  ["listing-service"]="-Xms128m -Xmx384m"
  ["search-service"]="-Xms64m -Xmx256m"
  ["booking-service"]="-Xms128m -Xmx384m"
  ["payment-service"]="-Xms64m -Xmx192m"
  ["review-service"]="-Xms64m -Xmx192m"
  ["media-service"]="-Xms64m -Xmx192m"
  ["notification-service"]="-Xms64m -Xmx128m"
)

# Common JVM flags for all services
COMMON_OPTS="-XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp"

start_service() {
  local service=$1
  local heap="${HEAP_SETTINGS[$service]}"

  if [ -z "$heap" ]; then
    echo "Unknown service: $service"
    echo "Available: ${!HEAP_SETTINGS[@]}"
    return 1
  fi

  echo "Starting $service with $heap $COMMON_OPTS"
  cd "$SAFAR_HOME/services/$service"
  MAVEN_OPTS="$heap $COMMON_OPTS" mvn spring-boot:run &
  echo "$service started (PID: $!)"
}

if [ "$1" = "all" ]; then
  for svc in "${!HEAP_SETTINGS[@]}"; do
    start_service "$svc"
    sleep 3
  done
  echo "All services started. Total max heap: ~2.4GB"
  wait
elif [ -n "$1" ]; then
  start_service "$1"
else
  echo "Usage: $0 <service-name|all>"
  echo "Services: ${!HEAP_SETTINGS[@]}"
fi
