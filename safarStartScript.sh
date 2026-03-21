#!/bin/bash

# 1. Define your base directory
BASE_DIR="/c/Users/Win-10/safar-platform/services"

# 2. List your projects
PROJECTS=("api-gateway" "auth-service" "user-service" "listing-service" "search-service" "booking-service" "payment-service" "review-service" "media-service" "notification-service")

for project in "${PROJECTS[@]}"
do
    echo "Preparing to launch: $project"
    
    # Define the full path
    FULL_PATH="$BASE_DIR/$project"
    
    # Check if directory exists before proceeding
    if [ ! -d "$FULL_PATH" ]; then
        echo "Warning: Directory $FULL_PATH not found. Skipping..."
        continue
    fi

    # Create the temporary shell script
    # Note: We use single quotes for the echo to safely wrap the double quotes inside
    echo "cd \"$FULL_PATH\" && mvn clean spring-boot:run \"-Dspring-boot.run.jvmArguments=-XX:+UseG1GC -XX:ParallelGCThreads=2 -Xms512m -Xmx512m\"" > "run_$project.sh"
    
    chmod +x "run_$project.sh"

    # Start Git Bash and tell it to run that specific file
    # We use 'cmd /c start' to trigger a new window correctly from bash
    start "" "C:\Program Files\Git\git-bash.exe" -c "./run_$project.sh"
    
    # Optional: Small sleep to prevent windows from overlapping instantly
    #sleep 2
done
