# Terraform Apply Status — 2026-03-22 00:28

**AWS Account:** `624627192872` (user: `sawank.sit`)
**Region:** `ap-south-1` (Mumbai)
**Domain:** `ysafar.com` (changed from `safar.com`)

## Outputs

```
alb_dns_name        = safar-production-alb-1235914325.ap-south-1.elb.amazonaws.com
ecs_cluster_name    = safar-production-cluster
vpc_id              = vpc-078363e67da36b64a
route53_zone_id     = Z03179051FKL0D1Y2X830
media_bucket        = safar-media-production
sns_alerts_topic    = arn:aws:sns:ap-south-1:624627192872:safar-production-alerts
redis_endpoint      = master.safar-redis.o0qdzo.aps1.cache.amazonaws.com
rds_endpoint        = (missing — see issues below)

route53_nameservers:
  - ns-1368.awsdns-43.org
  - ns-1621.awsdns-10.co.uk
  - ns-334.awsdns-41.com
  - ns-982.awsdns-58.net

ecr_repositories:
  ai-service           = 624627192872.dkr.ecr.ap-south-1.amazonaws.com/safar/ai-service
  api-gateway          = 624627192872.dkr.ecr.ap-south-1.amazonaws.com/safar/api-gateway
  auth-service         = 624627192872.dkr.ecr.ap-south-1.amazonaws.com/safar/auth-service
  booking-service      = 624627192872.dkr.ecr.ap-south-1.amazonaws.com/safar/booking-service
  listing-service      = 624627192872.dkr.ecr.ap-south-1.amazonaws.com/safar/listing-service
  media-service        = 624627192872.dkr.ecr.ap-south-1.amazonaws.com/safar/media-service
  messaging-service    = 624627192872.dkr.ecr.ap-south-1.amazonaws.com/safar/messaging-service
  notification-service = 624627192872.dkr.ecr.ap-south-1.amazonaws.com/safar/notification-service
  payment-service      = 624627192872.dkr.ecr.ap-south-1.amazonaws.com/safar/payment-service
  review-service       = 624627192872.dkr.ecr.ap-south-1.amazonaws.com/safar/review-service
  search-service       = 624627192872.dkr.ecr.ap-south-1.amazonaws.com/safar/search-service
  user-service         = 624627192872.dkr.ecr.ap-south-1.amazonaws.com/safar/user-service
```

## Resources — 108 Total

| Category | Count | Details |
|----------|-------|---------|
| VPC | 14 | VPC, 2 public + 2 private subnets, IGW, NAT Gateway, route tables |
| ECS | 2 | Cluster `safar-production-cluster` + Fargate capacity provider |
| ECR | 24 | 12 repos + 12 lifecycle policies (all services) |
| ALB | 3 | Load balancer + HTTP listener + API gateway target group |
| RDS | 2 | PostgreSQL 16 subnet group + parameter group |
| Redis | 2 | ElastiCache replication group + subnet group |
| MSK | 1 | Serverless Kafka cluster |
| Route 53 | 3 | Zone `ysafar.com` + 2 ACM validation records |
| ACM Certs | 2 | `ysafar.com` + `*.ysafar.com` (CloudFront us-east-1 + ALB ap-south-1) |
| S3 | 3 | Media bucket + CORS + ALB logs policy |
| CloudFront | 2 | OAC for media + admin CDN |
| Secrets Manager | 10 | 5 secrets + 5 versions (DB password, JWT, AES key, Razorpay, Mail) |
| Security Groups | 5 | ALB, ECS tasks, RDS, Redis, MSK |
| Service Discovery | 13 | Private DNS namespace + 12 service entries |
| CloudWatch | 16 | 12 log groups + 4 alarms (ALB 5xx, ECS unhealthy x3, Redis evictions) |
| SNS | 1 | Alerts topic `safar-production-alerts` |
| IAM | 6 | ECS execution role + task role + 4 policies (secrets, logs, MSK, S3) |

## Issues

### 1. ACM Certificates — PENDING_VALIDATION
Both `ysafar.com` certs are still pending DNS validation.
Set this CNAME in Namecheap (or wherever DNS is managed):
```
_5622cfa89f2a3a0ac9a05237dc4cbbae.ysafar.com → _c73eea9639f8fd1d0aaf0f805341184d.jkddzztszm.acm-validations.aws
```

### 2. RDS Endpoint Missing from Outputs
The `rds_endpoint` output returned "not found". The DB instance (`aws_db_instance.main`) may not have been created yet or there's a state mismatch. Run:
```bash
terraform state show aws_db_instance.main
```

### 3. No ECS Task Definitions or Services Yet
Only the cluster exists. To deploy services:
```bash
./deploy.sh build        # Build & push all Docker images to ECR
./deploy.sh deploy       # Create ECS services with force-new-deployment
```

### 4. Deposed Resource
Old `safar.com` CloudFront cert (id: `9bdc1360-a201-405d-9b37-db5437875c01`) still in state as a deposed object from the domain rename. Clean up with:
```bash
terraform state rm 'aws_acm_certificate.cloudfront[deposed]'
```

### 5. Route 53 Nameservers
Set these as Custom DNS in Namecheap for `ysafar.com`:
```
ns-1368.awsdns-43.org
ns-1621.awsdns-10.co.uk
ns-334.awsdns-41.com
ns-982.awsdns-58.net
```

## Next Steps
1. Set Route 53 nameservers in Namecheap
2. Wait for ACM cert validation (auto after DNS propagates)
3. Fix RDS endpoint issue (re-apply or check state)
4. Clean up deposed cert resource
5. Build & push Docker images (`./deploy.sh build`)
6. Deploy services to ECS (`./deploy.sh deploy`)
7. Subscribe email/Slack to SNS alerts topic
