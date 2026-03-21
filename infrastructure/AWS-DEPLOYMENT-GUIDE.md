# Safar Platform — AWS Deployment Guide

## Architecture Overview

```
                        ┌─────────────────┐
                        │   Route 53      │
                        │  safar.in       │
                        └────────┬────────┘
                                 │
                        ┌────────▼────────┐
                        │  CloudFront     │
                        │  (CDN + SSL)    │
                        └───┬────────┬────┘
                            │        │
               ┌────────────▼┐  ┌────▼──────────┐
               │ S3 (Web +   │  │ ALB           │
               │ Admin static)│  │ (API traffic) │
               └──────────────┘  └───────┬───────┘
                                         │
                              ┌──────────▼──────────┐
                              │   ECS Fargate        │
                              │   (12 services)      │
                              │                      │
                              │ api-gateway    :8080  │
                              │ auth-service   :8888  │
                              │ user-service   :8092  │
                              │ listing-service:8083  │
                              │ search-service :8084  │
                              │ booking-service:8095  │
                              │ payment-service:8086  │
                              │ review-service :8087  │
                              │ media-service  :8088  │
                              │ notification   :8089  │
                              │ messaging      :8091  │
                              │ ai-service     :8090  │
                              └──────────┬──────────┘
                                         │
                    ┌────────────┬────────┼────────┬──────────┐
                    │            │        │        │          │
              ┌─────▼──┐  ┌─────▼──┐ ┌───▼───┐ ┌──▼───┐ ┌───▼────┐
              │RDS     │  │Elasti- │ │Amazon │ │MSK   │ │S3      │
              │Postgres│  │Cache   │ │Open-  │ │Kafka │ │Media   │
              │  16    │  │Redis 7 │ │Search │ │      │ │Bucket  │
              └────────┘  └────────┘ └───────┘ └──────┘ └────────┘
```

---

## 1. AWS Services Mapping

| Local Component | AWS Service | Tier (Start) | Est. Monthly Cost |
|----------------|-------------|--------------|-------------------|
| PostgreSQL 16 | **RDS PostgreSQL** | db.t3.medium (Multi-AZ) | ~$140 |
| Redis 7 | **ElastiCache Redis** | cache.t3.small | ~$50 |
| Kafka + Zookeeper | **Amazon MSK** (Serverless) | Serverless | ~$80-150 |
| Elasticsearch 8 | **Amazon OpenSearch** | t3.medium.search (2 nodes) | ~$140 |
| 11 Java Services | **ECS Fargate** | 0.5 vCPU / 1GB each | ~$250 |
| 1 Python Service | **ECS Fargate** | 0.25 vCPU / 0.5GB | ~$15 |
| Next.js Web | **S3 + CloudFront** (SSR on Lambda) or ECS | — | ~$20 |
| Admin Dashboard | **S3 + CloudFront** (static) | — | ~$5 |
| Mobile App | **Expo EAS Build** → App Stores | — | — |
| Media uploads | **S3** + existing CloudFront | — | ~$10 |
| DNS | **Route 53** | — | ~$1 |
| SSL | **ACM** (free) | — | $0 |
| Secrets | **AWS Secrets Manager** | — | ~$5 |
| CI/CD | **GitHub Actions** | — | Free tier |
| Logs | **CloudWatch Logs** | — | ~$20 |
| **Total estimate** | | | **~$740-890/mo** |

---

## 2. Prerequisites

```bash
# Install AWS CLI
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip && sudo ./aws/install

# Configure credentials
aws configure
# → AWS Access Key ID, Secret, Region (ap-south-1 for India)

# Install Terraform
brew install terraform   # or choco install terraform on Windows

# Install Docker (for building images)
# Already needed for local dev
```

---

## 3. Infrastructure Setup (Terraform)

All Terraform files are in `infrastructure/terraform/`.

### 3.1 File Structure

```
infrastructure/terraform/
├── main.tf                  # Provider, backend, locals
├── variables.tf             # All input variables with defaults
├── vpc.tf                   # VPC, subnets, NAT gateway
├── security-groups.tf       # SGs for ALB, ECS, RDS, Redis, OpenSearch, MSK
├── rds.tf                   # PostgreSQL 16 Multi-AZ
├── redis.tf                 # ElastiCache Redis 7.1
├── opensearch.tf            # Amazon OpenSearch 2.11
├── msk.tf                   # MSK Serverless (Kafka)
├── ecr.tf                   # 12 ECR repositories + lifecycle policies
├── ecs.tf                   # Fargate cluster, task defs, auto-scaling
├── alb.tf                   # ALB, HTTPS listener, ACM certs
├── cloudfront.tf            # CDN for media + admin dashboard
├── s3.tf                    # Media bucket + admin bucket
├── iam.tf                   # ECS execution + task IAM roles
├── secrets.tf               # Secrets Manager entries
├── monitoring.tf            # CloudWatch alarms + SNS alerts
├── route53.tf               # DNS records (commented until domain ready)
├── outputs.tf               # Endpoint outputs
├── terraform.tfvars.example # Secret values template
└── .gitignore               # Excludes .terraform, tfstate, tfvars
```

### 3.2 VPC & Networking

- **Region:** `ap-south-1` (Mumbai) — lowest latency for India
- **VPC:** `10.0.0.0/16`
- **2 Availability Zones:** `ap-south-1a`, `ap-south-1b`
- **Private subnets:** `10.0.1.0/24`, `10.0.2.0/24` (ECS, RDS, Redis, OpenSearch, MSK)
- **Public subnets:** `10.0.101.0/24`, `10.0.102.0/24` (ALB, NAT Gateway)
- **Single NAT Gateway** to start (set `single_nat_gateway = false` for HA)

### 3.3 RDS PostgreSQL

- **Instance:** `db.t3.medium`, Multi-AZ, encrypted
- **Storage:** 50GB gp3, auto-scales to 200GB
- **Backups:** 7-day retention, 03:00-04:00 UTC window
- **Performance Insights** enabled
- **Flyway** creates schemas on first boot: `auth`, `users`, `listings`, `bookings`, `payments`, `reviews`, `messages`

### 3.4 ElastiCache Redis

- **Node type:** `cache.t3.small`
- **2 nodes:** Primary + 1 replica with automatic failover
- **TLS enabled** (transit + at-rest encryption)
- **Snapshot retention:** 3 days

### 3.5 Amazon OpenSearch

- **Engine:** OpenSearch 2.11 (Elasticsearch 8 compatible)
- **2 nodes:** `t3.medium.search`, zone-aware
- **30GB gp3** EBS per node
- **Enforce HTTPS**, TLS 1.2 minimum
- **Note:** Change `spring.elasticsearch.uris` to `https://<opensearch-endpoint>` in prod

### 3.6 Amazon MSK Serverless
 
- **Serverless** — pay per throughput, no idle cost
- **IAM authentication** (no SASL/SCRAM)
- **Requires** `aws-msk-iam-auth` Maven dependency in Java services

### 3.7 ECS Fargate

- **12 services** with individual task definitions
- **Service discovery** via Cloud Map (`*.safar.local`)
- **Auto-scaling** per CPU target (70%)
- **Deployment circuit breaker** with automatic rollback
- **Container Insights** enabled

### 3.8 Security Groups

| SG | Allows |
|----|--------|
| ALB | HTTP/HTTPS from internet |
| ECS Tasks | ALB → 8080-8095, inter-service (self) |
| RDS | PostgreSQL (5432) from ECS |
| Redis | Redis (6379) from ECS |
| OpenSearch | HTTPS (443) from ECS |
| MSK | Kafka (9098) from ECS |

---

## 4. Dockerfiles

### 4.1 Java Services (11 services)

Each Java service has an identical multi-stage Dockerfile:

```
services/<service>/Dockerfile
```

**Build stage:** Eclipse Temurin JDK 17 Alpine → `mvn clean package -DskipTests`
**Runtime stage:** Eclipse Temurin JRE 17 Alpine, non-root `safar` user, curl for health checks

**JVM flags:**
- `-XX:+UseContainerSupport` — respects container memory limits
- `-XX:MaxRAMPercentage=75.0` — uses 75% of container memory for heap
- `-Djava.security.egd=file:/dev/./urandom` — faster startup

### 4.2 Python AI Service

```
services/ai-service/Dockerfile
```

Multi-stage Python 3.11-slim, 2 uvicorn workers, non-root user.

### 4.3 Next.js Web Frontend

```
frontend/web/Dockerfile
```

3-stage build (deps → build → runtime). Requires `output: 'standalone'` in `next.config.mjs` (already added).

### 4.4 Admin Dashboard

Static build, deployed directly to S3 (no Dockerfile needed):
```bash
cd admin && npm run build
aws s3 sync dist/ s3://safar-admin-production/ --delete
```

---

## 5. Secrets Management

All secrets are stored in **AWS Secrets Manager** and injected into ECS containers at runtime.

| Secret | Path | Used By |
|--------|------|---------|
| DB password | `safar-production/db-password` | All DB services |
| JWT secret | `safar-production/jwt-secret` | All services |
| AES-256 key | `safar-production/aes-encryption-key` | user-service |
| Razorpay | `safar-production/razorpay` | payment-service |
| Mail credentials | `safar-production/mail-credentials` | notification-service |

Secrets are referenced in ECS task definitions via `secrets` (not `environment`), so they never appear in task definition JSON or Docker images.

---

## 6. CI/CD Pipeline (GitHub Actions)

### 6.1 Workflows

| File | Trigger | What it does |
|------|---------|--------------|
| `ci.yml` | PR / push to develop | Build + test all services (Java, Python, Web, Admin) |
| `deploy-services.yml` | Push to main (services/) | Auto-detect changed services → build → push ECR → deploy ECS |
| `deploy-web.yml` | Push to main (frontend/web/) | Build Next.js → push ECR → deploy ECS |
| `deploy-admin.yml` | Push to main (admin/) | Build → sync S3 → invalidate CloudFront |

### 6.2 Required GitHub Secrets

| Secret | Description |
|--------|-------------|
| `AWS_DEPLOY_ROLE_ARN` | IAM role ARN for OIDC-based deployment |
| `ADMIN_CLOUDFRONT_DIST_ID` | CloudFront distribution ID for admin cache invalidation |

### 6.3 Required GitHub Variables

| Variable | Example |
|----------|---------|
| `NEXT_PUBLIC_API_URL` | `https://api.safar.in` |
| `NEXT_PUBLIC_CDN_DOMAIN` | `media.safar.in` |
| `VITE_API_URL` | `https://api.safar.in` |

---

## 7. Service Configuration for Production

Each service needs an `application-prod.yml` profile. Key differences from dev:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:5432/safar?currentSchema=${SCHEMA_NAME}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
  redis:
    ssl:
      enabled: true            # ElastiCache requires TLS
  kafka:
    properties:
      security.protocol: SASL_SSL
      sasl.mechanism: AWS_MSK_IAM
      sasl.jaas.config: >
        software.amazon.msk.auth.iam.IAMLoginModule required;
      sasl.client.callback.handler.class: >
        software.amazon.msk.auth.iam.IAMClientCallbackHandler
  elasticsearch:
    uris: https://${OPENSEARCH_ENDPOINT}:443

logging:
  level:
    root: WARN
    com.safar: INFO
```

**Add MSK IAM auth dependency** to each service's `pom.xml`:
```xml
<dependency>
  <groupId>software.amazon.msk</groupId>
  <artifactId>aws-msk-iam-auth</artifactId>
  <version>2.0.3</version>
</dependency>
```

---

## 8. DNS & SSL Setup

### Domains

| Subdomain | Points To | Purpose |
|-----------|-----------|---------|
| `api.safar.in` | ALB | Backend API (all 12 services via gateway) |
| `www.safar.in` | CloudFront / ECS | Next.js web frontend |
| `admin.safar.in` | CloudFront → S3 | Admin dashboard |
| `media.safar.in` | CloudFront → S3 | Property photos, media CDN |

### SSL Certificates

- **ALB cert:** ACM in `ap-south-1` (auto-created by Terraform)
- **CloudFront cert:** ACM in `us-east-1` (required by CloudFront)
- Both use DNS validation via Route 53

### Steps

1. Register `safar.in` in Route 53 (or transfer nameservers)
2. Uncomment Route 53 records in `route53.tf`
3. Run `terraform apply` — certificates auto-validate via DNS

---

## 9. Deployment Sequence

### Phase 1 — Infrastructure (Terraform)

```bash
cd infrastructure/terraform

# 1. Copy and fill in secrets
cp terraform.tfvars.example terraform.tfvars
# Edit terraform.tfvars with real values

# 2. Initialize
terraform init

# 3. Preview changes
terraform plan -out=plan.tfplan

# 4. Apply (~15-20 minutes)
terraform apply plan.tfplan
```

This creates: VPC, RDS, Redis, OpenSearch, MSK, ECR, ECS cluster, ALB, CloudFront, S3 buckets, Secrets Manager, CloudWatch alarms.

### Phase 2 — Build & Push Docker Images

```bash
# Login to ECR
aws ecr get-login-password --region ap-south-1 | docker login --username AWS --password-stdin 624627192872.dkr.ecr.ap-south-1.amazonaws.com

# Build and push all services
for svc in api-gateway auth-service user-service listing-service \
  search-service booking-service payment-service review-service \
  media-service notification-service messaging-service ai-service; do
  docker build -t 624627192872.dkr.ecr.ap-south-1.amazonaws.com/safar/$svc:v1 services/$svc
  docker push 624627192872.dkr.ecr.ap-south-1.amazonaws.com/safar/$svc:v1
done

# Or use the deploy script:
./infrastructure/scripts/deploy.sh build
```

### Phase 3 — Deploy Services (order matters)

```bash
# Deploy in dependency order
./infrastructure/scripts/deploy.sh deploy auth-service
./infrastructure/scripts/deploy.sh deploy user-service
./infrastructure/scripts/deploy.sh deploy listing-service
./infrastructure/scripts/deploy.sh deploy search-service
./infrastructure/scripts/deploy.sh deploy booking-service
./infrastructure/scripts/deploy.sh deploy payment-service
./infrastructure/scripts/deploy.sh deploy review-service
./infrastructure/scripts/deploy.sh deploy media-service
./infrastructure/scripts/deploy.sh deploy notification-service
./infrastructure/scripts/deploy.sh deploy messaging-service
./infrastructure/scripts/deploy.sh deploy ai-service
./infrastructure/scripts/deploy.sh deploy api-gateway  # Last — depends on all others
```

### Phase 4 — Deploy Frontends

```bash
# Admin dashboard → S3
./infrastructure/scripts/deploy.sh admin

# Web frontend → ECS (or S3 if using static export)
docker build -t <ecr>/safar/web-frontend:v1 frontend/web
docker push <ecr>/safar/web-frontend:v1
# Deploy via ECS

# Mobile → Expo EAS
cd frontend/mobile
eas build --platform all
eas submit --platform all
```

### Phase 5 — Verify

```bash
# Check service status
./infrastructure/scripts/deploy.sh status

# Test API
curl https://api.safar.in/actuator/health

# Tail logs
./infrastructure/scripts/deploy.sh logs api-gateway
```

---

## 10. Scaling Strategy

| Service | Initial | Auto-scale Target | Max |
|---------|---------|-------------------|-----|
| api-gateway | 2 | CPU > 70% | 6 |
| auth-service | 2 | CPU > 70% | 4 |
| listing-service | 2 | CPU > 70% | 4 |
| search-service | 2 | CPU > 70% | 6 |
| booking-service | 2 | CPU > 70% | 4 |
| payment-service | 1 | CPU > 70% | 3 |
| user-service | 1 | CPU > 70% | 3 |
| review-service | 1 | CPU > 70% | 2 |
| media-service | 1 | CPU > 70% | 3 |
| notification-service | 1 | CPU > 70% | 2 |
| messaging-service | 1 | CPU > 70% | 2 |
| ai-service | 1 | CPU > 70% | 2 |

Auto-scaling is configured in `ecs.tf` with:
- **Scale-out cooldown:** 60 seconds (react fast)
- **Scale-in cooldown:** 300 seconds (avoid thrashing)

---

## 11. Monitoring & Alerts

### CloudWatch Alarms (configured in `monitoring.tf`)

| Alarm | Threshold | Action |
|-------|-----------|--------|
| RDS CPU high | > 80% for 15 min | SNS alert |
| RDS storage low | < 10 GB | SNS alert |
| ALB 5xx errors | > 50 in 5 min | SNS alert |
| ECS task down | 0 running tasks | SNS alert |
| Redis evictions | > 100 in 5 min | SNS alert |
| OpenSearch RED | Cluster status RED | SNS alert |

### Subscribe to Alerts

```bash
# After terraform apply, subscribe your email:
aws sns subscribe \
  --topic-arn <sns-alerts-topic-arn-from-outputs> \
  --protocol email \
  --notification-endpoint your@email.com
```

### CloudWatch Dashboards (manual setup recommended)

- **ECS:** CPU, memory, task count per service
- **RDS:** Connections, IOPS, replication lag
- **ElastiCache:** Hit rate, memory usage, evictions
- **OpenSearch:** Cluster health, indexing rate, search latency
- **ALB:** Request count, 5xx rate, p99 latency

---

## 12. Cost Optimization

| Strategy | Savings | When |
|----------|---------|------|
| **Fargate Savings Plan** (1-year) | ~40% on compute | After traffic stabilizes |
| **RDS Reserved Instance** (1-year, no upfront) | ~30% | After launch |
| **MSK Serverless** | Pay-per-use only | Already configured |
| **Single NAT Gateway** | ~$35/mo vs dual | Until HA needed |
| **Fargate Spot** for non-critical services | ~70% | notification, ai-service |
| **S3 Intelligent-Tiering** | Auto-tier cold media | Already configured |
| **CloudFront caching** | Reduces origin hits | Already configured |
| **ECR lifecycle policies** | Avoids storage bloat | Already configured |

---

## 13. Scaffolded Files Summary

### Terraform (19 files)

| File | Purpose |
|------|---------|
| `main.tf` | Provider config, locals, S3 backend (commented) |
| `variables.tf` | All variables with service sizing defaults |
| `vpc.tf` | VPC, 2 AZs, public/private subnets, NAT gateway |
| `security-groups.tf` | 6 security groups (ALB, ECS, RDS, Redis, OpenSearch, MSK) |
| `rds.tf` | PostgreSQL 16, Multi-AZ, encrypted, 50-200GB |
| `redis.tf` | ElastiCache Redis 7.1, 2 nodes, TLS, failover |
| `opensearch.tf` | OpenSearch 2.11, 2 nodes, zone-aware |
| `msk.tf` | MSK Serverless with IAM auth |
| `ecr.tf` | 12 ECR repos + lifecycle policies |
| `ecs.tf` | Fargate cluster, 12 task defs, service discovery, auto-scaling |
| `alb.tf` | ALB with HTTPS, HTTP→HTTPS redirect, ACM certs |
| `cloudfront.tf` | CDN for media + admin static hosting |
| `s3.tf` | Media bucket (versioned, intelligent-tiering) + admin bucket |
| `iam.tf` | ECS execution + task roles (S3, MSK, OpenSearch, Secrets, Logs) |
| `secrets.tf` | 5 Secrets Manager entries |
| `monitoring.tf` | 6 CloudWatch alarms + SNS topic |
| `route53.tf` | DNS records (commented until domain ready) |
| `outputs.tf` | All endpoint outputs |
| `terraform.tfvars.example` | Secret values template |

### Dockerfiles (13 files)

| Service | Port | Base Image |
|---------|------|------------|
| api-gateway | 8080 | eclipse-temurin:17-jre-alpine |
| auth-service | 8888 | eclipse-temurin:17-jre-alpine |
| user-service | 8092 | eclipse-temurin:17-jre-alpine |
| listing-service | 8083 | eclipse-temurin:17-jre-alpine |
| search-service | 8084 | eclipse-temurin:17-jre-alpine |
| booking-service | 8095 | eclipse-temurin:17-jre-alpine |
| payment-service | 8086 | eclipse-temurin:17-jre-alpine |
| review-service | 8087 | eclipse-temurin:17-jre-alpine |
| media-service | 8088 | eclipse-temurin:17-jre-alpine |
| notification-service | 8089 | eclipse-temurin:17-jre-alpine |
| messaging-service | 8091 | eclipse-temurin:17-jre-alpine |
| ai-service | 8090 | python:3.11-slim |
| web frontend | 3000 | node:20-alpine |

### GitHub Actions (4 workflows)

| Workflow | Trigger | Action |
|----------|---------|--------|
| `ci.yml` | PR / push to develop | Build + test all (Java, Python, Web, Admin) |
| `deploy-services.yml` | Push to main (services/) | Auto-detect → ECR → ECS deploy |
| `deploy-web.yml` | Push to main (frontend/web/) | Build → ECR → ECS |
| `deploy-admin.yml` | Push to main (admin/) | Build → S3 → CloudFront invalidation |

### Other Files

| File | Purpose |
|------|---------|
| `infrastructure/scripts/deploy.sh` | CLI deploy tool (init/plan/apply/build/deploy/status/logs) |
| `.dockerignore` | Excludes .git, node_modules, target/, .env files |
| `infrastructure/terraform/.gitignore` | Excludes .terraform, tfstate, tfvars |
| `frontend/web/next.config.mjs` | Added `output: 'standalone'` for Docker |

---

## 14. Quick Reference — Deploy Script

```bash
./infrastructure/scripts/deploy.sh init          # Terraform init
./infrastructure/scripts/deploy.sh plan          # Preview infra changes
./infrastructure/scripts/deploy.sh apply         # Apply infra changes
./infrastructure/scripts/deploy.sh build         # Build & push ALL Docker images
./infrastructure/scripts/deploy.sh build auth-service  # Build specific service
./infrastructure/scripts/deploy.sh deploy        # Deploy ALL to ECS
./infrastructure/scripts/deploy.sh deploy api-gateway  # Deploy specific service
./infrastructure/scripts/deploy.sh admin         # Deploy admin to S3
./infrastructure/scripts/deploy.sh status        # Check ECS service health
./infrastructure/scripts/deploy.sh logs api-gateway    # Tail service logs
```
