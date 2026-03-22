# ── Outputs ──────────────────────────────────────────────────────────────────

output "vpc_id" {
  value = module.vpc.vpc_id
}

output "route53_zone_id" {
  description = "Route 53 hosted zone ID"
  value       = aws_route53_zone.main.zone_id
}

output "route53_nameservers" {
  description = "NS records — set these in Namecheap Custom DNS"
  value       = aws_route53_zone.main.name_servers
}

output "alb_dns_name" {
  description = "ALB DNS name — point api.ysafar.com CNAME here"
  value       = aws_lb.main.dns_name
}

output "rds_endpoint" {
  description = "RDS PostgreSQL endpoint"
  value       = aws_db_instance.main.address
  sensitive   = true
}

output "redis_endpoint" {
  description = "ElastiCache Redis primary endpoint"
  value       = aws_elasticache_replication_group.main.primary_endpoint_address
  sensitive   = true
}

# Disabled — enable after OpenSearch subscription
# output "opensearch_endpoint" {
#   description = "OpenSearch domain endpoint"
#   value       = aws_opensearch_domain.main.endpoint
#   sensitive   = true
# }

output "msk_bootstrap_brokers" {
  description = "MSK Serverless bootstrap brokers"
  value       = aws_msk_serverless_cluster.main.arn
  sensitive   = true
}

output "ecr_repositories" {
  description = "ECR repository URLs for each service"
  value       = { for k, v in aws_ecr_repository.services : k => v.repository_url }
}

output "media_bucket" {
  value = aws_s3_bucket.media.id
}

output "media_cdn_domain" {
  description = "CloudFront domain for media — point media.safar.in here"
  value       = aws_cloudfront_distribution.media.domain_name
}

output "admin_cdn_domain" {
  description = "CloudFront domain for admin — point admin.safar.in here"
  value       = aws_cloudfront_distribution.admin.domain_name
}

output "ecs_cluster_name" {
  value = aws_ecs_cluster.main.name
}

output "sns_alerts_topic_arn" {
  description = "Subscribe your email/Slack to this topic for alerts"
  value       = aws_sns_topic.alerts.arn
}
