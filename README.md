# Safar Platform

Global property rental marketplace — India MVP.

## Quick Start

### Prerequisites
- Java 17+
- Maven 3.9+
- Docker Desktop

### 1. Configure Environment

```bash
cp .env.example .env
# Edit .env with your local credentials
```

Key values to set in `.env`:
- `JWT_SECRET` — generate with `openssl rand -base64 32`
- `DB_PASSWORD` — default `safar_dev` matches docker-compose
- `RAZORPAY_KEY_ID / KEY_SECRET` — use Razorpay test keys

### 2. Start Infrastructure

```bash
docker-compose up -d
```

Services started: PostgreSQL 16, Redis 7, Kafka, Zookeeper, Elasticsearch 8.

### 3. Run a Service

```bash
cd services/auth-service && mvn spring-boot:run
cd services/user-service && mvn spring-boot:run
cd services/listing-service && mvn spring-boot:run
```

### 4. Run All Tests

```bash
mvn clean test
```

All tests use H2 in-memory database (test profile) — no running infrastructure needed.

## API Base URLs (local)

| Service         | URL                              | Port |
|-----------------|----------------------------------|------|
| API Gateway     | http://localhost:8080            | 8080 |
| Auth Service    | http://localhost:8081/api/v1     | 8081 |
| User Service    | http://localhost:8082/api/v1     | 8082 |
| Listing Service | http://localhost:8083/api/v1     | 8083 |

## Postman Collection

Import `docs/postman/sprint-1.json` into Postman for a ready-to-run collection of all Sprint 1 endpoints with test assertions.

## Project Structure

```
safar-platform/
├── services/
│   ├── api-gateway/       # Spring Cloud Gateway, JWT validation
│   ├── auth-service/      # OTP login, JWT (port 8081)
│   ├── user-service/      # Profiles, host subscriptions (port 8082)
│   └── listing-service/   # Properties, availability (port 8083)
├── docs/
│   └── postman/           # Postman collections per sprint
├── .env.example           # Environment variable template
├── docker-compose.yml     # Local infrastructure
└── pom.xml                # Parent Maven POM
```

## Subscription Tiers

| Tier       | Price/month | Max Listings | Commercial |
|------------|-------------|--------------|------------|
| STARTER    | ₹999        | 2            | No         |
| PRO        | ₹2,499      | 10           | No         |
| COMMERCIAL | ₹3,999      | Unlimited    | Yes        |

All tiers include a **90-day free trial**.
