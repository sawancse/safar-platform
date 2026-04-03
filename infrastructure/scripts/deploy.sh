#!/usr/bin/env bash
set -euo pipefail

# ── Safar Platform — AWS Deployment Script ──────────────────────────────────
# Usage:
#   ./deploy.sh init          — First-time Terraform init
#   ./deploy.sh plan          — Preview infrastructure changes
#   ./deploy.sh apply         — Apply infrastructure changes
#   ./deploy.sh build [svc]   — Build & push Docker images (all or specific)
#   ./deploy.sh deploy [svc]  — Deploy services to ECS (all or specific)
#   ./deploy.sh admin         — Deploy admin dashboard to S3
#   ./deploy.sh status        — Check ECS service status
#   ./deploy.sh logs [svc]    — Tail CloudWatch logs for a service

AWS_REGION="${AWS_REGION:-ap-south-1}"
ECS_CLUSTER="${ECS_CLUSTER:-safar-production-cluster}"
PROJECT="safar"

SERVICES=(
  api-gateway auth-service user-service listing-service
  search-service booking-service payment-service review-service
  media-service notification-service messaging-service ai-service
  chef-service
)

ECR_REGISTRY="$(aws sts get-caller-identity --query Account --output text).dkr.ecr.${AWS_REGION}.amazonaws.com"

# ── Helpers ──────────────────────────────────────────────────────────────────

log()  { echo -e "\033[1;32m▸\033[0m $*"; }
err()  { echo -e "\033[1;31m✗\033[0m $*" >&2; }
warn() { echo -e "\033[1;33m⚠\033[0m $*"; }

ensure_ecr_login() {
  aws ecr get-login-password --region "$AWS_REGION" \
    | docker login --username AWS --password-stdin "$ECR_REGISTRY" 2>/dev/null
}

# ── Commands ─────────────────────────────────────────────────────────────────

cmd_init() {
  log "Initializing Terraform..."
  cd infrastructure/terraform
  terraform init
}

cmd_plan() {
  log "Planning infrastructure changes..."
  cd infrastructure/terraform
  terraform plan -out=plan.tfplan
}

cmd_apply() {
  log "Applying infrastructure changes..."
  cd infrastructure/terraform
  terraform apply plan.tfplan
}

cmd_build() {
  local targets=("${@:-${SERVICES[@]}}")
  ensure_ecr_login

  for svc in "${targets[@]}"; do
    log "Building ${svc}..."
    IMAGE="${ECR_REGISTRY}/${PROJECT}/${svc}"
    TAG="sha-$(git rev-parse --short HEAD)"

    if [ "$svc" = "ai-service" ]; then
      docker build -t "${IMAGE}:${TAG}" -t "${IMAGE}:latest" "services/${svc}"
    else
      docker build -t "${IMAGE}:${TAG}" -t "${IMAGE}:latest" -f "services/${svc}/Dockerfile" .
    fi
    docker push "${IMAGE}:${TAG}"
    docker push "${IMAGE}:latest"
    log "${svc} pushed → ${IMAGE}:${TAG}"
  done
}

cmd_deploy() {
  local targets=("${@:-${SERVICES[@]}}")

  for svc in "${targets[@]}"; do
    log "Deploying ${svc}..."
    aws ecs update-service \
      --cluster "$ECS_CLUSTER" \
      --service "$svc" \
      --force-new-deployment \
      --query 'service.serviceName' \
      --output text
  done

  log "Waiting for services to stabilize..."
  for svc in "${targets[@]}"; do
    aws ecs wait services-stable --cluster "$ECS_CLUSTER" --services "$svc" &
  done
  wait
  log "All services deployed successfully!"
}

cmd_admin() {
  log "Building admin dashboard..."
  cd admin
  npm ci
  npm run build

  log "Deploying to S3..."
  aws s3 sync dist/ "s3://${PROJECT}-admin-production/" \
    --delete \
    --cache-control "public, max-age=31536000, immutable" \
    --exclude "index.html"

  aws s3 cp dist/index.html "s3://${PROJECT}-admin-production/index.html" \
    --cache-control "no-cache, no-store, must-revalidate"

  log "Admin dashboard deployed!"
}

cmd_status() {
  log "ECS Service Status:"
  aws ecs list-services --cluster "$ECS_CLUSTER" --query 'serviceArns[*]' --output text \
    | tr '\t' '\n' | while read -r arn; do
      name=$(echo "$arn" | rev | cut -d'/' -f1 | rev)
      status=$(aws ecs describe-services --cluster "$ECS_CLUSTER" --services "$name" \
        --query 'services[0].{desired:desiredCount,running:runningCount,status:status}' \
        --output text)
      echo "  $name: $status"
    done
}

cmd_logs() {
  local svc="${1:?Usage: deploy.sh logs <service-name>}"
  log "Tailing logs for ${svc}..."
  aws logs tail "/ecs/${PROJECT}/${svc}" --follow --since 5m
}

# ── Main ─────────────────────────────────────────────────────────────────────

case "${1:-help}" in
  init)    cmd_init ;;
  plan)    cmd_plan ;;
  apply)   cmd_apply ;;
  build)   shift; cmd_build "$@" ;;
  deploy)  shift; cmd_deploy "$@" ;;
  admin)   cmd_admin ;;
  status)  cmd_status ;;
  logs)    shift; cmd_logs "$@" ;;
  *)
    echo "Usage: $0 {init|plan|apply|build|deploy|admin|status|logs} [service]"
    exit 1
    ;;
esac
