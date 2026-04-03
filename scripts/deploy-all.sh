#!/bin/bash
# ════════════════════════════════════════════════════════════════
# Safar Platform — Full Deployment Script
# Deploys all changed services since 2026-03-25
# ════════════════════════════════════════════════════════════════
# Usage:
#   ./scripts/deploy-all.sh              # Full deploy (build + migrate + deploy)
#   ./scripts/deploy-all.sh --build-only # Just build & push images
#   ./scripts/deploy-all.sh --deploy-only # Just ECS deploy (images already pushed)
#   ./scripts/deploy-all.sh --migrate-only # Just run Flyway migrations
# ════════════════════════════════════════════════════════════════

set -euo pipefail

# ── Config ─────────────────────────────────────────────────────
AWS_REGION="ap-south-1"
ECS_CLUSTER="safar-production-cluster"
PROJECT="safar"
IMAGE_TAG="sha-$(git rev-parse --short HEAD)-$(date +%Y%m%d%H%M)"

# Services that need rebuild (changed since Mar 25)
JAVA_SERVICES=(
  "api-gateway"
  "listing-service"
  "booking-service"
  "user-service"
  "search-service"
)
PYTHON_SERVICES=("ai-service")
ALL_SERVICES=("${JAVA_SERVICES[@]}" "${PYTHON_SERVICES[@]}")

# Frontend
WEB_SERVICE="web-frontend"
ADMIN_BUCKET="safar-admin-production"

# Colors
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; NC='\033[0m'
info()  { echo -e "${BLUE}[INFO]${NC} $1"; }
ok()    { echo -e "${GREEN}[OK]${NC} $1"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
err()   { echo -e "${RED}[ERROR]${NC} $1"; }
step()  { echo -e "\n${YELLOW}════════════════════════════════════════${NC}"; echo -e "${YELLOW}  $1${NC}"; echo -e "${YELLOW}════════════════════════════════════════${NC}\n"; }

# ── Parse args ─────────────────────────────────────────────────
BUILD=true; MIGRATE=true; DEPLOY=true
case "${1:-}" in
  --build-only)   MIGRATE=false; DEPLOY=false ;;
  --deploy-only)  BUILD=false; MIGRATE=false ;;
  --migrate-only) BUILD=false; DEPLOY=false ;;
esac

# ── Prerequisites ──────────────────────────────────────────────
step "0. Checking prerequisites"

command -v aws >/dev/null || { err "AWS CLI not found"; exit 1; }
command -v docker >/dev/null || { err "Docker not found"; exit 1; }
command -v jq >/dev/null || { err "jq not found. Install: choco install jq"; exit 1; }

# Verify AWS credentials
AWS_ACCOUNT=$(aws sts get-caller-identity --query Account --output text 2>/dev/null)
if [ -z "$AWS_ACCOUNT" ]; then
  err "AWS credentials not configured. Run: aws configure"
  exit 1
fi
ECR_REGISTRY="${AWS_ACCOUNT}.dkr.ecr.${AWS_REGION}.amazonaws.com"
ok "AWS Account: $AWS_ACCOUNT, Region: $AWS_REGION"
ok "ECR: $ECR_REGISTRY"
ok "Image Tag: $IMAGE_TAG"

# ── ECR Login ──────────────────────────────────────────────────
step "1. Logging into ECR"
aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $ECR_REGISTRY
ok "ECR login successful"

# ════════════════════════════════════════════════════════════════
# PHASE 1: BUILD & PUSH
# ════════════════════════════════════════════════════════════════
if [ "$BUILD" = true ]; then
  step "2. Building & pushing Docker images"

  REPO_ROOT=$(git rev-parse --show-toplevel)
  cd "$REPO_ROOT"

  # Build Java services
  for SERVICE in "${JAVA_SERVICES[@]}"; do
    info "Building $SERVICE..."
    IMAGE="$ECR_REGISTRY/$PROJECT/$SERVICE"

    # Docker build from repo root (Dockerfiles use services/$SERVICE/ paths)
    docker build -f services/$SERVICE/Dockerfile -t $IMAGE:$IMAGE_TAG -t $IMAGE:latest .
    docker push $IMAGE:$IMAGE_TAG
    docker push $IMAGE:latest
    ok "Pushed: $IMAGE:$IMAGE_TAG"
  done

  # Build Python service (ai-service)
  for SERVICE in "${PYTHON_SERVICES[@]}"; do
    info "Building $SERVICE..."
    IMAGE="$ECR_REGISTRY/$PROJECT/$SERVICE"
    docker build -t $IMAGE:$IMAGE_TAG -t $IMAGE:latest services/$SERVICE
    docker push $IMAGE:$IMAGE_TAG
    docker push $IMAGE:latest
    ok "Pushed: $IMAGE:$IMAGE_TAG"
  done

  # Build web frontend
  info "Building web frontend..."
  WEB_IMAGE="$ECR_REGISTRY/$PROJECT/$WEB_SERVICE"
  docker build \
    --build-arg NEXT_PUBLIC_API_URL=https://api.ysafar.com \
    -t $WEB_IMAGE:$IMAGE_TAG -t $WEB_IMAGE:latest frontend/web
  docker push $WEB_IMAGE:$IMAGE_TAG
  docker push $WEB_IMAGE:latest
  ok "Pushed: web-frontend"

  # Build & deploy admin to S3
  info "Building admin dashboard..."
  cd admin
  npm ci --silent
  VITE_API_URL=https://api.ysafar.com npm run build 2>&1 | tail -3
  ok "Admin built"

  info "Syncing admin to S3..."
  aws s3 sync dist/ s3://$ADMIN_BUCKET/ --delete \
    --cache-control "public, max-age=31536000, immutable" \
    --exclude "index.html" --exclude "*.json"
  aws s3 cp dist/index.html s3://$ADMIN_BUCKET/index.html \
    --cache-control "no-cache, no-store, must-revalidate"
  ok "Admin deployed to S3"

  # Invalidate CloudFront
  ADMIN_CF_ID=$(aws cloudfront list-distributions --query "DistributionList.Items[?contains(Origins.Items[0].DomainName,'admin')].Id" --output text 2>/dev/null || echo "")
  if [ -n "$ADMIN_CF_ID" ]; then
    aws cloudfront create-invalidation --distribution-id $ADMIN_CF_ID --paths "/*" > /dev/null
    ok "CloudFront cache invalidated"
  fi

  cd "$REPO_ROOT"
  ok "All images built and pushed"
fi

# ════════════════════════════════════════════════════════════════
# PHASE 2: DATABASE MIGRATIONS
# ════════════════════════════════════════════════════════════════
if [ "$MIGRATE" = true ]; then
  step "3. Running Flyway migrations"

  # Get RDS endpoint from Terraform output or ECS task definition
  RDS_ENDPOINT=$(aws rds describe-db-instances \
    --query "DBInstances[?DBInstanceIdentifier=='safar-production'].Endpoint.Address" \
    --output text 2>/dev/null)

  if [ -z "$RDS_ENDPOINT" ]; then
    # Fallback: extract from existing task definition
    RDS_ENDPOINT=$(aws ecs describe-task-definition --task-definition safar-listing-service \
      --query "taskDefinition.containerDefinitions[0].environment[?name=='DB_HOST'].value" \
      --output text 2>/dev/null || echo "")
  fi

  if [ -z "$RDS_ENDPOINT" ]; then
    warn "Could not determine RDS endpoint. Skipping migrations."
    warn "Flyway will run automatically on service startup (Spring Boot)."
  else
    DB_PASSWORD=$(aws secretsmanager get-secret-value --secret-id safar/production/db_password \
      --query SecretString --output text 2>/dev/null || echo "")

    if [ -n "$DB_PASSWORD" ]; then
      info "RDS: $RDS_ENDPOINT"

      # Migration: user-service (V24-V25: referrals, loyalty)
      info "Migrating user-service (V24-V25)..."
      docker run --rm \
        -v "$REPO_ROOT/services/user-service/src/main/resources/db/migration:/flyway/sql" \
        flyway/flyway:10 \
        -url="jdbc:postgresql://$RDS_ENDPOINT:5432/safar?currentSchema=users" \
        -user=safar_admin -password="$DB_PASSWORD" \
        -schemas=users migrate 2>&1 | tail -5
      ok "user-service migrated"

      # Migration: booking-service (V30: penalty config)
      info "Migrating booking-service (V30)..."
      docker run --rm \
        -v "$REPO_ROOT/services/booking-service/src/main/resources/db/migration:/flyway/sql" \
        flyway/flyway:10 \
        -url="jdbc:postgresql://$RDS_ENDPOINT:5432/safar?currentSchema=bookings" \
        -user=safar_admin -password="$DB_PASSWORD" \
        -schemas=bookings migrate 2>&1 | tail -5
      ok "booking-service migrated"

      # Migration: listing-service (V51-V56: sale properties, builder)
      info "Migrating listing-service (V51-V56)..."
      docker run --rm \
        -v "$REPO_ROOT/services/listing-service/src/main/resources/db/migration:/flyway/sql" \
        flyway/flyway:10 \
        -url="jdbc:postgresql://$RDS_ENDPOINT:5432/safar?currentSchema=listings" \
        -user=safar_admin -password="$DB_PASSWORD" \
        -schemas=listings migrate 2>&1 | tail -5
      ok "listing-service migrated"

      ok "All migrations complete"
    else
      warn "Could not retrieve DB password. Flyway will run on service startup."
    fi
  fi
fi

# ════════════════════════════════════════════════════════════════
# PHASE 3: DEPLOY TO ECS
# ════════════════════════════════════════════════════════════════
if [ "$DEPLOY" = true ]; then
  step "4. Deploying services to ECS"

  deploy_ecs_service() {
    local SERVICE=$1
    info "Deploying $SERVICE..."

    # Get current task definition
    TASK_DEF=$(aws ecs describe-task-definition \
      --task-definition safar-$SERVICE \
      --query 'taskDefinition' --output json 2>/dev/null)

    if [ -z "$TASK_DEF" ] || [ "$TASK_DEF" = "null" ]; then
      warn "Task definition safar-$SERVICE not found. Skipping."
      return
    fi

    NEW_IMAGE="$ECR_REGISTRY/$PROJECT/$SERVICE:$IMAGE_TAG"

    # Update image in task definition
    NEW_TASK_DEF=$(echo $TASK_DEF | jq \
      --arg IMAGE "$NEW_IMAGE" \
      '.containerDefinitions[0].image = $IMAGE |
       del(.taskDefinitionArn, .revision, .status, .requiresAttributes,
           .compatibilities, .registeredAt, .registeredBy)')

    # Register new task definition
    NEW_TASK_ARN=$(aws ecs register-task-definition \
      --cli-input-json "$NEW_TASK_DEF" \
      --query 'taskDefinition.taskDefinitionArn' --output text)

    # Update service
    aws ecs update-service \
      --cluster $ECS_CLUSTER \
      --service $SERVICE \
      --task-definition $NEW_TASK_ARN \
      --output text > /dev/null

    ok "Deployed $SERVICE (task: $NEW_TASK_ARN)"
  }

  # Deploy all changed backend services
  for SERVICE in "${ALL_SERVICES[@]}"; do
    deploy_ecs_service $SERVICE
  done

  # Deploy web frontend
  deploy_ecs_service $WEB_SERVICE

  # ── Wait for all services to stabilize ──
  step "5. Waiting for services to stabilize"

  ALL_DEPLOY_SERVICES=("${ALL_SERVICES[@]}" "$WEB_SERVICE")
  for SERVICE in "${ALL_DEPLOY_SERVICES[@]}"; do
    info "Waiting for $SERVICE..."
    aws ecs wait services-stable \
      --cluster $ECS_CLUSTER \
      --services $SERVICE 2>/dev/null && ok "$SERVICE stable" || warn "$SERVICE may still be deploying"
  done

  # ── Create/verify ES indexes ──
  step "6. Creating Elasticsearch indexes"

  # Get ES endpoint from search-service
  ES_HOST=$(aws ecs describe-task-definition --task-definition safar-search-service \
    --query "taskDefinition.containerDefinitions[0].environment[?name=='ELASTICSEARCH_URI'].value" \
    --output text 2>/dev/null || echo "http://elasticsearch.safar.local:9200")

  info "ES host: $ES_HOST"
  info "Note: ES indexes (sale_properties, builder_projects) will be auto-created"
  info "by IndexInitializer on search-service startup."
  info "To force reindex, call:"
  info "  POST https://api.ysafar.com/api/v1/sale-properties/admin/reindex"
  info "  POST https://api.ysafar.com/api/v1/builder-projects/admin/reindex"
  ok "ES indexes will be created on startup"
fi

# ════════════════════════════════════════════════════════════════
# PHASE 4: VERIFICATION
# ════════════════════════════════════════════════════════════════
step "7. Verifying deployment"

API_BASE="https://api.ysafar.com"

check_health() {
  local name=$1 url=$2
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$url" --connect-timeout 5 2>/dev/null || echo "000")
  if [ "$STATUS" = "200" ]; then
    ok "$name: healthy ($STATUS)"
  else
    warn "$name: $STATUS (may still be starting)"
  fi
}

info "Checking service health via API gateway..."
check_health "API Gateway"       "$API_BASE/actuator/health"
check_health "Auth"              "$API_BASE/api/v1/auth/health"
check_health "Search"            "$API_BASE/api/v1/search/listings?query=test&size=1"
check_health "Sale Properties"   "$API_BASE/api/v1/search/sale-properties?size=1"
check_health "Builder Projects"  "$API_BASE/api/v1/search/builder-projects?size=1"
check_health "Locality Trends"   "$API_BASE/api/v1/locality-trends/city?city=Bangalore"

# ════════════════════════════════════════════════════════════════
# DONE
# ════════════════════════════════════════════════════════════════
step "DEPLOYMENT COMPLETE"

echo -e "${GREEN}Services deployed:${NC}"
for S in "${ALL_SERVICES[@]}"; do echo "  - $S"; done
echo "  - $WEB_SERVICE"
echo "  - admin (S3 + CloudFront)"
echo ""
echo -e "${GREEN}Migrations applied:${NC}"
echo "  - user-service: V24 (referrals), V25 (loyalty tiers)"
echo "  - booking-service: V30 (penalty config + max cap)"
echo "  - listing-service: V51-V56 (sale properties, inquiries, visits, buyer profiles, price history, builder projects)"
echo ""
echo -e "${GREEN}New features live:${NC}"
echo "  - Buy/Sell marketplace: /buy, /buy/search, /buy/[id], /sell"
echo "  - Builder projects: /projects, /projects/[id], /builder/new-project"
echo "  - Referral system: /api/v1/referrals/*"
echo "  - Loyalty tiers: /api/v1/loyalty/*"
echo "  - Agreement PDF: /api/v1/pg-tenancies/{id}/agreement/pdf"
echo "  - Dynamic pricing: /api/v1/ai/pricing/competitor-comparison, /occupancy-pricing"
echo ""
echo -e "${YELLOW}Post-deploy checklist:${NC}"
echo "  1. Reindex sale properties: curl -X POST $API_BASE/api/v1/sale-properties/admin/reindex -H 'Authorization: Bearer \$TOKEN'"
echo "  2. Reindex builder projects: curl -X POST $API_BASE/api/v1/builder-projects/admin/reindex -H 'Authorization: Bearer \$TOKEN'"
echo "  3. Verify Kafka topics created: sale.property.indexed, sale.property.deleted, builder.project.indexed"
echo "  4. Test agreement PDF: curl $API_BASE/api/v1/pg-tenancies/{tenancyId}/agreement/pdf -o test.pdf"
echo "  5. Check admin panel: https://admin.ysafar.com/sale-properties"
echo ""
echo "Image tag: $IMAGE_TAG"
echo "Deployed at: $(date)"
