#!/bin/bash

# 1. Define your base directory (Using standard Linux path)
BASE_DIR="$HOME/safar-platform/services"

# 2. List your projects
PROJECTS=(
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
)

# JVM Arguments
JVM_OPTS="-XX:+UseG1GC -XX:ParallelGCThreads=2 -Xms512m -Xmx512m"

echo "🚀 Starting Microservices in new tabs..."

for project in "${PROJECTS[@]}"
do
    FULL_PATH="$BASE_DIR/$project"
    
    # Check if directory exists
    if [ ! -d "$FULL_PATH" ]; then
        echo "⚠️  Warning: $FULL_PATH not found. Skipping $project..."
        continue
    fi

    echo "Opening $project..."

    # Launch a new GNOME Terminal tab for each service
    # --tab: opens in a new tab of the existing window
    # --title: sets the tab name to the project name
    # -- bash -c: executes the command and stays open if it fails
    gnome-terminal --tab --title="$project" -- bash -c "
        echo 'Launching $project...';
        cd '$FULL_PATH' || exit;
        mvn clean spring-boot:run -Dspring-boot.run.jvmArguments='$JVM_OPTS';
        exec bash"

    # Small delay to avoid overloading the CPU during the initial 'mvn clean'
    sleep 1.5
done

echo "✨ All service tabs triggered."
