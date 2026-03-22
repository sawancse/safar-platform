FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# Copy parent pom (strip modules to avoid missing-child errors)
COPY pom.xml ./pom.xml
RUN apk add --no-cache maven sed     && sed -i '/<modules>/,/<\/modules>/d' pom.xml     && mvn -N install -q 2>/dev/null || true

# Copy service pom and resolve dependencies
COPY services/review-service/pom.xml ./services/review-service/pom.xml
RUN mvn -f services/review-service/pom.xml dependency:go-offline -B -q 2>/dev/null || true

# Copy source and build
COPY services/review-service/src ./services/review-service/src
RUN mvn -f services/review-service/pom.xml clean package -DskipTests -q

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S safar && adduser -S safar -G safar && apk add --no-cache curl
USER safar
COPY --from=build /app/services/review-service/target/*.jar app.jar
EXPOSE 8087
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
