# Safar Platform — AWS Operations Guide

## 1. Infrastructure Commands

```bash
# ── Terraform (Infrastructure as Code) ──
cd C:/Users/Win-10/safar-platform/infrastructure/terraform
./terraform.exe init                    # First-time setup
./terraform.exe plan                    # Preview changes
./terraform.exe apply -auto-approve     # Apply changes
./terraform.exe state list              # List managed resources
./terraform.exe output                  # Show endpoints/URLs

# ── Docker Build & Push ──
ECR=624627192872.dkr.ecr.ap-south-1.amazonaws.com
aws ecr get-login-password --region ap-south-1 | docker login --username AWS --password-stdin $ECR

# Build single service
docker build --no-cache -t $ECR/safar/api-gateway:latest -f services/api-gateway/Dockerfile .

# Push to ECR
docker push $ECR/safar/api-gateway:latest

# Build all services
cd C:/Users/Win-10/safar-platform && ./infrastructure/scripts/deploy.sh build

# ── ECS Deploy ──
# Deploy single service
aws ecs update-service --cluster safar-production-cluster --service api-gateway --force-new-deployment

# Deploy all
./infrastructure/scripts/deploy.sh deploy

# Check status
aws ecs list-services --cluster safar-production-cluster --query 'serviceArns' --output text | tr '\t' '\n' | while read arn; do name=$(echo "$arn" | awk -F'/' '{print $NF}'); status=$(aws ecs describe-services --cluster safar-production-cluster --services "$name" --query 'services[0].{desired:desiredCount,running:runningCount}' --output text); echo "$name: $status"; done

# ── Admin Dashboard Deploy ──
cd admin && npm run build
aws s3 sync dist/ s3://safar-admin-production/ --delete --cache-control "public, max-age=31536000, immutable" --exclude "index.html"
aws s3 cp dist/index.html s3://safar-admin-production/index.html --cache-control "no-cache, no-store, must-revalidate"

# ── DNS ──
MSYS_NO_PATHCONV=1 aws route53 list-resource-record-sets --hosted-zone-id Z03179051FKL0D1Y2X830 --query "ResourceRecordSets[*].[Name,Type]" --output table
```

## 2. Debug & Database Commands

```bash
# ── Get RDS Endpoint ──
cd C:/Users/Win-10/safar-platform/infrastructure/terraform
./terraform.exe output rds_endpoint

# ── Connect to RDS (from local — needs VPN or bastion) ──
# RDS is in private subnet, not directly accessible. Options:
# Option A: SSH tunnel via bastion host
# Option B: Use AWS Session Manager + port forwarding
aws ssm start-session --target <bastion-instance-id> --document-name AWS-StartPortForwardingSessionToRemoteHost --parameters '{"host":["<rds-endpoint>"],"portNumber":["5432"],"localPortNumber":["5433"]}'
# Then connect locally:
psql -h localhost -p 5433 -U safar_admin -d safar

# ── Connect to RDS via ECS Exec (run psql inside a container) ──
# First enable ECS Exec on a service:
aws ecs update-service --cluster safar-production-cluster --service auth-service --enable-execute-command
# Get running task ID:
TASK=$(aws ecs list-tasks --cluster safar-production-cluster --service-name auth-service --query 'taskArns[0]' --output text)
# Exec into container:
aws ecs execute-command --cluster safar-production-cluster --task $TASK --container auth-service --interactive --command "/bin/sh"

# ── Query DB Schemas ──
# Once connected to psql:
\dn                           # List all schemas
\dt auth.*                    # List tables in auth schema
\dt users.*                   # List tables in users schema
\dt listings.*                # List tables in listings schema
\dt bookings.*                # List tables in bookings schema
\dt payments.*                # List tables in payments schema
\dt reviews.*                 # List tables in reviews schema
\dt notifications.*           # List tables in notifications schema
\dt messages.*                # List tables in messages schema
SELECT * FROM auth.users LIMIT 5;
SELECT * FROM listings.listings LIMIT 5;

# ── Get Redis Endpoint ──
./terraform.exe output redis_endpoint

# ── Connect to Redis (via ECS Exec) ──
# From inside a container with redis-cli:
redis-cli -h <redis-endpoint> -p 6379 --tls
KEYS *                        # List all keys
GET otp:rate:<phone>          # Check rate limit
TTL <key>                     # Check expiry

# ── Get MSK Kafka Endpoint ──
MSYS_NO_PATHCONV=1 aws kafka get-bootstrap-brokers --cluster-arn "arn:aws:kafka:ap-south-1:624627192872:cluster/safar-production-kafka/e8221ac7-be46-4526-aeae-0917750f9d58-s3"

# ── Check ECS Task Events (why a service is failing) ──
aws ecs describe-services --cluster safar-production-cluster --services <service-name> --query 'services[0].events[0:5].[message]' --output text

# ── Check Stopped Task Reason ──
TASK=$(aws ecs list-tasks --cluster safar-production-cluster --service-name <service-name> --desired-status STOPPED --query 'taskArns[0]' --output text)
aws ecs describe-tasks --cluster safar-production-cluster --tasks $TASK --query 'tasks[0].{reason:stoppedReason,containerReason:containers[0].reason,exitCode:containers[0].exitCode}' --output json

# ── Secrets Manager (view secrets) ──
aws secretsmanager get-secret-value --secret-id safar-production/jwt-secret --query 'SecretString' --output text
aws secretsmanager get-secret-value --secret-id safar-production/db-password --query 'SecretString' --output text
aws secretsmanager get-secret-value --secret-id safar-production/razorpay --query 'SecretString' --output text
aws secretsmanager get-secret-value --secret-id safar-production/mail-credentials --query 'SecretString' --output text
aws secretsmanager get-secret-value --secret-id safar-production/aes-encryption-key --query 'SecretString' --output text

# ── Update a secret ──
aws secretsmanager update-secret --secret-id safar-production/db-password --secret-string "new-password"
```

## 3. Logs for All Services

```bash
# ── View logs for a specific service ──
# Note: On Git Bash (Windows), use MSYS_NO_PATHCONV=1 prefix

# Tail live logs
MSYS_NO_PATHCONV=1 aws logs tail "/ecs/safar/api-gateway" --follow --since 5m

# Get latest log stream
MSYS_NO_PATHCONV=1 aws logs describe-log-streams \
  --log-group-name "/ecs/safar/<service-name>" \
  --order-by LastEventTime --descending --max-items 1 \
  --query 'logStreams[0].logStreamName' --output text

# Read logs from a stream
MSYS_NO_PATHCONV=1 aws logs get-log-events \
  --log-group-name "/ecs/safar/<service-name>" \
  --log-stream-name "<stream-name>" \
  --limit 50 --query 'events[*].message' --output text

# ── Quick script: errors from all services ──
for svc in api-gateway auth-service user-service listing-service search-service booking-service payment-service review-service media-service notification-service messaging-service ai-service; do
  echo "=== $svc ==="
  stream=$(MSYS_NO_PATHCONV=1 aws logs describe-log-streams --log-group-name "/ecs/safar/$svc" --order-by LastEventTime --descending --max-items 1 --query 'logStreams[0].logStreamName' --output text 2>/dev/null)
  [ -n "$stream" ] && [ "$stream" != "None" ] && MSYS_NO_PATHCONV=1 aws logs get-log-events --log-group-name "/ecs/safar/$svc" --log-stream-name "$stream" --limit 10 --query 'events[*].message' --output text 2>&1 | grep -i "error\|exception" | head -3
  echo ""
done

# ── Log group names ──
# /ecs/safar/api-gateway
# /ecs/safar/auth-service
# /ecs/safar/user-service
# /ecs/safar/listing-service
# /ecs/safar/search-service
# /ecs/safar/booking-service
# /ecs/safar/payment-service
# /ecs/safar/review-service
# /ecs/safar/media-service
# /ecs/safar/notification-service
# /ecs/safar/messaging-service
# /ecs/safar/ai-service
```

## 4. Credentials & Endpoints

```
# ── Database (RDS PostgreSQL 16) ──
Host:     <run: terraform output rds_endpoint>
Port:     5432
Database: safar
Username: safar_admin
Password: <in Secrets Manager: safar-production/db-password>
Schemas:  auth, users, listings, bookings, payments, reviews, notifications, messages

# ── Redis (ElastiCache) ──
Host:     <run: terraform output redis_endpoint>
Port:     6379
TLS:      enabled
Auth:     none (VPC-only access)

# ── Kafka (MSK Serverless) ──
Bootstrap: boot-2ege0pbm.c3.kafka-serverless.ap-south-1.amazonaws.com:9098
Auth:      IAM (automatic via task role)
Protocol:  SASL_SSL

# ── Elasticsearch ──
Status:   NOT DEPLOYED (OpenSearch commented out in Terraform)
Current:  search-service using placeholder URL
TODO:     Enable aws_opensearch_domain in opensearch.tf or use self-managed ES

# ── S3 ──
Media bucket:  safar-media-production
Admin bucket:  safar-admin-production
ALB logs:      safar-alb-logs-production
Auth:          IAM task role (no explicit keys needed)

# ── CloudFront CDN ──
Media:    d40begvouhqsy.cloudfront.net  → media.ysafar.com
Admin:    d2fl04pl2b7q9p.cloudfront.net → admin.ysafar.com

```

## 5. AWS Secrets Manager (Key Vault)

### List All Secrets
```bash
aws secretsmanager list-secrets --query 'SecretList[*].[Name,ARN]' --output table
```

### Read Secret Values
```bash
# JWT Secret (plain string)
aws secretsmanager get-secret-value --secret-id safar-production/jwt-secret --query 'SecretString' --output text

# DB Password (plain string)
aws secretsmanager get-secret-value --secret-id safar-production/db-password --query 'SecretString' --output text

# AES Encryption Key (plain string)
aws secretsmanager get-secret-value --secret-id safar-production/aes-encryption-key --query 'SecretString' --output text

# Razorpay (JSON — contains key_id, key_secret, webhook_secret)
aws secretsmanager get-secret-value --secret-id safar-production/razorpay --query 'SecretString' --output text
# Parse individual fields:
aws secretsmanager get-secret-value --secret-id safar-production/razorpay --query 'SecretString' --output text | python -c "import sys,json; d=json.load(sys.stdin); print('key_id:', d['key_id']); print('key_secret:', d['key_secret']); print('webhook_secret:', d['webhook_secret'])"

# Mail Credentials (JSON — contains username, password)
aws secretsmanager get-secret-value --secret-id safar-production/mail-credentials --query 'SecretString' --output text
```

### Update Secret Values
```bash
# Update plain string secrets
aws secretsmanager update-secret --secret-id safar-production/jwt-secret --secret-string "new-jwt-secret-base64"
aws secretsmanager update-secret --secret-id safar-production/db-password --secret-string "new-db-password"
aws secretsmanager update-secret --secret-id safar-production/aes-encryption-key --secret-string "new-aes-256-key-base64"

# Update Razorpay (JSON)
aws secretsmanager update-secret --secret-id safar-production/razorpay --secret-string '{"key_id":"rzp_live_xxx","key_secret":"xxx","webhook_secret":"xxx"}'

# Update Mail Credentials (JSON)
aws secretsmanager update-secret --secret-id safar-production/mail-credentials --secret-string '{"username":"noreply@ysafar.com","password":"app-password"}'
```

### After Updating Secrets
ECS tasks cache secrets at startup. To pick up new values, redeploy the affected service:
```bash
# Redeploy single service
aws ecs update-service --cluster safar-production-cluster --service <service-name> --force-new-deployment

# Redeploy all services
for svc in api-gateway auth-service user-service listing-service search-service booking-service payment-service review-service media-service notification-service messaging-service ai-service; do
  aws ecs update-service --cluster safar-production-cluster --service "$svc" --force-new-deployment --query 'service.serviceName' --output text
done
```

### Secret → Service Mapping
| Secret | Used By |
|--------|---------|
| `safar-production/jwt-secret` | All 12 services |
| `safar-production/db-password` | auth, user, listing, booking, payment, review, notification, messaging |
| `safar-production/aes-encryption-key` | user-service |
| `safar-production/razorpay` | payment-service, user-service |
| `safar-production/mail-credentials` | auth-service, notification-service |

### Create a New Secret
```bash
# Plain string
aws secretsmanager create-secret --name safar-production/my-new-secret --secret-string "secret-value"

# JSON
aws secretsmanager create-secret --name safar-production/my-new-secret --secret-string '{"key1":"value1","key2":"value2"}'
```

### Delete a Secret
```bash
# Soft delete (recoverable for 7 days)
aws secretsmanager delete-secret --secret-id safar-production/my-secret --recovery-window-in-days 7

# Force delete (immediate, not recoverable)
aws secretsmanager delete-secret --secret-id safar-production/my-secret --force-delete-without-recovery
```

### Rotate a Secret
```bash
# Generate new JWT secret
openssl rand -base64 32

# Generate new AES-256 key
openssl rand -base64 32

# Generate new DB password
openssl rand -base64 16

# Then update + redeploy
aws secretsmanager update-secret --secret-id safar-production/jwt-secret --secret-string "$(openssl rand -base64 32)"
# WARNING: Rotating JWT secret will invalidate all active user sessions!
```

```
# ── JWT ──
Secret:   <in Secrets Manager: safar-production/jwt-secret>
Algo:     HMAC-SHA384
Access:   60 min
Refresh:  30 days (stored in Redis)

# ── Razorpay ──
Key ID:          <in Secrets Manager: safar-production/razorpay → key_id>
Key Secret:      <in Secrets Manager: safar-production/razorpay → key_secret>
Webhook Secret:  <in Secrets Manager: safar-production/razorpay → webhook_secret>

# ── Mail (SMTP) ──
Host:     smtp.gmail.com
Port:     587
Username: <in Secrets Manager: safar-production/mail-credentials → username>
Password: <in Secrets Manager: safar-production/mail-credentials → password>

# ── AES Encryption (PII) ──
Key:      <in Secrets Manager: safar-production/aes-encryption-key>

# ── AWS Account ──
Account ID: 624627192872
Region:     ap-south-1 (Mumbai)
IAM User:   sawank.sit
ECR:        624627192872.dkr.ecr.ap-south-1.amazonaws.com

# ── ECS ──
Cluster:  safar-production-cluster
Services: 12 (all on Fargate, 512 CPU / 1024 MB each)

# ── DNS ──
Domain:       ysafar.com
Registrar:    Namecheap
Nameservers:  Route53 (Z03179051FKL0D1Y2X830)
Subdomains:   api.ysafar.com (ALB), admin.ysafar.com (CF), media.ysafar.com (CF)

# ── Amplify (Frontend) ──
App URL:  https://master.d2xwyyu5ngq13t.amplifyapp.com
Custom:   ysafar.com (pending SSL)
Repo:     sawancse/safar-web
```
