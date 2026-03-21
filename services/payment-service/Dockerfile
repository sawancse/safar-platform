FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY pom.xml ./pom.xml
COPY services/payment-service/pom.xml ./services/payment-service/pom.xml
RUN apk add --no-cache maven \
    && mvn -pl services/payment-service -am dependency:go-offline -B -q 2>/dev/null || true
COPY services/payment-service/src ./services/payment-service/src
RUN mvn -pl services/payment-service -am clean package -DskipTests -q -o 2>/dev/null \
    || mvn -pl services/payment-service -am clean package -DskipTests -q

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S safar && adduser -S safar -G safar && apk add --no-cache curl
USER safar
COPY --from=build /app/services/payment-service/target/*.jar app.jar
EXPOSE 8086
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
