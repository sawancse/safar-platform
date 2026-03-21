# ── Security Groups ──────────────────────────────────────────────────────────

resource "aws_security_group" "alb" {
  name        = "${local.name_prefix}-alb-sg"
  description = "ALB - allow HTTP/HTTPS from internet"
  vpc_id      = module.vpc.vpc_id

  ingress {
    description = "HTTP"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "HTTPS"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_security_group" "ecs_tasks" {
  name        = "${local.name_prefix}-ecs-tasks-sg"
  description = "ECS tasks - allow traffic from ALB and inter-service"
  vpc_id      = module.vpc.vpc_id

  # Allow all traffic between ECS tasks (inter-service communication)
  ingress {
    description = "Inter-service traffic"
    from_port   = 0
    to_port     = 65535
    protocol    = "tcp"
    self        = true
  }

  # Allow traffic from ALB
  ingress {
    description     = "ALB to ECS"
    from_port       = 8080
    to_port         = 8095
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_security_group" "rds" {
  name        = "${local.name_prefix}-rds-sg"
  description = "RDS - allow PostgreSQL from ECS tasks"
  vpc_id      = module.vpc.vpc_id

  ingress {
    description     = "PostgreSQL from ECS"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.ecs_tasks.id]
  }
}

resource "aws_security_group" "redis" {
  name        = "${local.name_prefix}-redis-sg"
  description = "ElastiCache - allow Redis from ECS tasks"
  vpc_id      = module.vpc.vpc_id

  ingress {
    description     = "Redis from ECS"
    from_port       = 6379
    to_port         = 6379
    protocol        = "tcp"
    security_groups = [aws_security_group.ecs_tasks.id]
  }
}

# Disabled — enable after OpenSearch subscription
# resource "aws_security_group" "opensearch" {
#   name        = "${local.name_prefix}-opensearch-sg"
#   description = "OpenSearch - allow HTTPS from ECS tasks"
#   vpc_id      = module.vpc.vpc_id
#
#   ingress {
#     description     = "HTTPS from ECS"
#     from_port       = 443
#     to_port         = 443
#     protocol        = "tcp"
#     security_groups = [aws_security_group.ecs_tasks.id]
#   }
# }

resource "aws_security_group" "msk" {
  name        = "${local.name_prefix}-msk-sg"
  description = "MSK - allow Kafka from ECS tasks"
  vpc_id      = module.vpc.vpc_id

  ingress {
    description     = "Kafka from ECS"
    from_port       = 9098
    to_port         = 9098
    protocol        = "tcp"
    security_groups = [aws_security_group.ecs_tasks.id]
  }
}
