# ── RDS PostgreSQL 16 ────────────────────────────────────────────────────────

resource "aws_db_subnet_group" "main" {
  name       = "${local.name_prefix}-db-subnet"
  subnet_ids = module.vpc.private_subnets
}

resource "aws_db_parameter_group" "postgres16" {
  name   = "${local.name_prefix}-pg16-params"
  family = "postgres16"

  parameter {
    name  = "log_min_duration_statement"
    value = "1000" # Log queries slower than 1s
  }

  parameter {
    name         = "shared_preload_libraries"
    value        = "pg_stat_statements"
    apply_method = "pending-reboot"
  }
}

resource "aws_db_instance" "main" {
  identifier = "${local.name_prefix}-db"

  engine         = "postgres"
  engine_version = "16.6"
  instance_class = "db.t3.micro" # MVP sizing (saves ~$40/mo; upgrade to t3.medium for prod)

  allocated_storage     = 50
  max_allocated_storage = 200
  storage_type          = "gp3"
  storage_encrypted     = true

  db_name  = "safar"
  username = "safar_admin"
  password = var.db_password

  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.rds.id]
  parameter_group_name   = aws_db_parameter_group.postgres16.name

  multi_az            = false # Single-AZ for MVP (saves ~$65/mo; enable for prod HA)
  publicly_accessible = false
  skip_final_snapshot = false

  final_snapshot_identifier = "${local.name_prefix}-db-final"

  backup_retention_period = 1 # Free tier limit; increase for prod
  backup_window           = "03:00-04:00"
  maintenance_window      = "sun:04:00-sun:05:00"

  performance_insights_enabled = false # Not free on t3.micro; enable for prod

  # Flyway creates schemas on first service boot:
  # auth, users, listings, bookings, payments, reviews, messages
}
