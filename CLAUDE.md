# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Safar is a property rental marketplace (India MVP). Multi-module monorepo with 10 Java/Spring Boot microservices, 1 Python/FastAPI AI service, and 3 frontend apps (Next.js web, Expo mobile, React/Vite admin).

## Build & Run Commands

### Infrastructure (Docker)
```bash
docker-compose up -d   # PostgreSQL 16, Redis 7, Kafka, Zookeeper, Elasticsearch 8
```

### Java Services (Maven)
```bash
mvn clean test                                    # Run all tests (uses H2, no infra needed)
mvn clean test -pl services/auth-service          # Run single service tests from root
mvn test -pl services/auth-service -Dtest=AuthServiceTest          # Single test class
mvn test -pl services/auth-service -Dtest=AuthServiceTest#testName # Single test method
cd services/auth-service && mvn spring-boot:run   # Run a single service
```

### AI Service (Python/FastAPI)
```bash
cd services/ai-service
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8090 --reload
```

### Frontend Web (Next.js)
```bash
cd frontend/web && npm run dev      # Dev server
cd frontend/web && npm run build    # Production build
cd frontend/web && npm run lint     # ESLint
```

### Mobile (Expo)
```bash
cd frontend/mobile && npm start       # Dev server
cd frontend/mobile && npm run android
cd frontend/mobile && npm run ios
```

### Admin Dashboard (React/Vite)
```bash
cd admin && npm run dev      # Vite dev server on port 3001
cd admin && npm run build
```

## Architecture

### Service Ports & Database Schemas
| Service | Port | DB Schema |
|---------|------|-----------|
| API Gateway | 8080 | — (no DB) |
| Auth | 8888 | `auth` |
| User | 8092 | `users` |
| Listing | 8083 | `listings` |
| Search | 8084 | — (Elasticsearch) |
| Booking | 8095 | `bookings` |
| Payment | 8086 | `payments` |
| Review | 8087 | `reviews` |
| Media | 8088 | — (S3) |
| Notification | 8089 | — (Kafka only) |
| AI (Python) | 8090 | — |

All services share a single PostgreSQL instance but use isolated schemas. Flyway migrations live at `src/main/resources/db/migration/V{n}__*.sql`.

### Inter-Service Communication
- **Synchronous**: REST via `RestTemplate`. Service URLs configured via `services.{name}.url` properties.
- **Asynchronous**: Kafka events. Key topics: `payment.captured`, `booking.created`, `booking.confirmed`, `booking.cancelled`, `media.uploaded`, `listing.indexed`.

### API Gateway (`services/api-gateway`)
- Spring Cloud Gateway (WebFlux-based, reactive — no `spring-boot-starter-web`).
- `JwtAuthFilter` (GlobalFilter) validates JWT and propagates `X-User-Id` and `X-User-Role` headers to downstream services.
- Public paths: `/api/v1/auth/**`, GET listings, search, payment webhooks.

### Authentication Flow (`services/auth-service`)
- OTP-based login. Dev mode hardcodes OTP as `"123456"`.
- JWT: HMAC-SHA384, 60-min access tokens, 30-day refresh tokens (UUID stored in Redis).
- Rate limiting: max 3 OTP requests/hour per phone via Redis (`otp:rate:{phone}`).
- Each downstream service also validates JWT locally via its own `JwtAuthFilter` + `SecurityConfig`.

### AI Service (`services/ai-service`)
- Python FastAPI app with two routers: `services/scout.py` (AI Scout) and `services/listing_generator.py`.
- Routed via API Gateway at `/api/v1/ai/**`.

### Java Service Layer Pattern
```
com.safar.{service}/
├── controller/    # REST endpoints (@RestController)
├── service/       # Business logic
├── repository/    # Spring Data JPA
├── entity/        # JPA entities (UUID PKs, @CreationTimestamp/@UpdateTimestamp)
├── dto/           # Request/Response records
├── config/        # SecurityConfig, Kafka, etc.
├── security/      # JwtAuthFilter
└── kafka/         # Event consumers (@KafkaListener)
```

### Key Technical Conventions
- **Java 17**, Spring Boot 3.4.0, Lombok, MapStruct 1.6.0 for DTO mapping.
- **Database**: PostgreSQL with Flyway migrations. `ddl-auto: validate` in prod — never `update`/`create`. Each service has its own schema.
- **Monetary values**: All prices stored in **paise** (1 INR = 100 paise) as `Long`.
- **API versioning**: All endpoints prefixed `/api/v1/`.
- **Error handling**: `@RestControllerAdvice` with Spring RFC 7807 `ProblemDetail` responses.
- **Testing**: JUnit 5 + Mockito. H2 in-memory DB for test profile (Flyway disabled, `ddl-auto: create-drop`). Redis/Kafka mocked.
- **UUID primary keys**: `@GeneratedValue(strategy = GenerationType.UUID)`.
- **OpenAPI**: springdoc-openapi on each service at `/api-docs` and `/swagger-ui.html`.

### Testing Gotchas
- Use `@ExtendWith(MockitoExtension.class)` — not `@SpringBootTest` — for unit tests.
- **SearchHits mocks**: Use `doReturn()` instead of `when()` to avoid Mockito nested-when issues. Build `SearchHits` mock objects in a helper method *before* passing to stubbing.
- **ElasticsearchOperations.search()**: Use `any(NativeQuery.class)` not bare `any()` to avoid ambiguous method resolution.
- **Razorpay JSONObject.put()**: Cast arguments to `(Object)` to resolve method ambiguity.
- **Elasticsearch 8.x range queries**: Use `r.number(n -> { n.field(...); n.gte(...); return n; })`.

### Frontend Conventions
- **Web**: Next.js 14 App Router, Tailwind CSS, Zustand state, Radix UI components, next-intl i18n (en/hi). Auth middleware protects `/dashboard`, `/host`, `/book` routes.
- **Mobile**: Expo 51, Expo Router (file-based), Zustand, Axios, offline queue support, UPI payment integration, Expo Secure Store for tokens.
- **Admin**: React 18 SPA, Vite, Ant Design, Axios, Recharts. Vite proxies `/api` to `localhost:8080`.
- All frontends use TypeScript strict mode, `@/*` path aliases, and target the API Gateway at `localhost:8080`.

### Environment
Copy `.env.example` to `.env`. Key values: `JWT_SECRET` (base64-encoded, shared across all services), `RAZORPAY_KEY_ID`/`KEY_SECRET`, `AWS_*` for media service, `TWILIO_*` for SMS OTP, database credentials. See `.env.example` for full list.
