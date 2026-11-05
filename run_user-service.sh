ENV_FILE="/c/Users/Win-10/safar-platform/.env"
if [ -f "$ENV_FILE" ]; then while IFS= read -r line; do line="${line%$'\r'}"; case "$line" in RAZORPAY_*=*) export "$line";; esac; done < "$ENV_FILE"; fi
cd "/c/Users/Win-10/safar-platform/services/user-service" && mvn clean spring-boot:run "-Dspring-boot.run.jvmArguments=-XX:+UseG1GC -XX:ParallelGCThreads=2 -Xms128m -Xmx128m"
