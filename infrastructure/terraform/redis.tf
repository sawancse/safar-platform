# ── ElastiCache Redis 7 ──────────────────────────────────────────────────────

resource "aws_elasticache_subnet_group" "main" {
  name       = "${local.name_prefix}-redis-subnet"
  subnet_ids = module.vpc.private_subnets
}

resource "aws_elasticache_replication_group" "main" {
  replication_group_id = "${var.project}-redis"
  description          = "Safar Redis - JWT refresh, rate limiting, caching"

  engine         = "redis"
  engine_version = "7.1"
  node_type      = "cache.t3.small"
  port           = 6379

  num_cache_clusters = 2 # Primary + 1 replica

  subnet_group_name  = aws_elasticache_subnet_group.main.name
  security_group_ids = [aws_security_group.redis.id]

  at_rest_encryption_enabled = true
  transit_encryption_enabled = true

  automatic_failover_enabled = true

  snapshot_retention_limit = 3
  snapshot_window          = "02:00-03:00"
  maintenance_window       = "sun:03:00-sun:04:00"
}
