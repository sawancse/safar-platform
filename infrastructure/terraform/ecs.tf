# ── ECS Cluster ──────────────────────────────────────────────────────────────

resource "aws_ecs_cluster" "main" {
  name = "${local.name_prefix}-cluster"

  setting {
    name  = "containerInsights"
    value = "disabled" # MVP cost saving (~$15/mo); enable for prod
  }
}

resource "aws_ecs_cluster_capacity_providers" "main" {
  cluster_name       = aws_ecs_cluster.main.name
  capacity_providers = ["FARGATE", "FARGATE_SPOT"]

  default_capacity_provider_strategy {
    capacity_provider = "FARGATE_SPOT"
    weight            = 4
    base              = 0
  }

  default_capacity_provider_strategy {
    capacity_provider = "FARGATE"
    weight            = 1
    base              = 1 # 1 task always on regular Fargate (api-gateway)
  }
}

# ── Service Discovery ────────────────────────────────────────────────────────

resource "aws_service_discovery_private_dns_namespace" "main" {
  name = "safar.local"
  vpc  = module.vpc.vpc_id
}

resource "aws_service_discovery_service" "services" {
  for_each = var.services

  name = each.key

  dns_config {
    namespace_id = aws_service_discovery_private_dns_namespace.main.id

    dns_records {
      ttl  = 10
      type = "A"
    }

    routing_policy = "MULTIVALUE"
  }

  health_check_custom_config {
    failure_threshold = 1
  }
}

# ── CloudWatch Log Groups ───────────────────────────────────────────────────

resource "aws_cloudwatch_log_group" "services" {
  for_each = var.services

  name              = "/ecs/${var.project}/${each.key}"
  retention_in_days = 7 # MVP cost saving; increase for prod
}

# ── ECS Task Definitions ────────────────────────────────────────────────────

resource "aws_ecs_task_definition" "services" {
  for_each = var.services

  family                   = "${var.project}-${each.key}"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = each.value.cpu
  memory                   = each.value.memory
  execution_role_arn       = aws_iam_role.ecs_execution.arn
  task_role_arn            = aws_iam_role.ecs_task.arn

  container_definitions = jsonencode([{
    name      = each.key
    image     = "${aws_ecr_repository.services[each.key].repository_url}:latest"
    essential = true

    portMappings = [{
      containerPort = each.value.port
      protocol      = "tcp"
    }]

    environment = concat(
      [
        { name = "SPRING_PROFILES_ACTIVE", value = "prod" },
        { name = "SERVER_PORT", value = tostring(each.value.port) },
        # Service discovery hostnames (used in application-prod.yml)
        { name = "AUTH_SERVICE_HOST", value = "auth-service.safar.local" },
        { name = "USER_SERVICE_HOST", value = "user-service.safar.local" },
        { name = "LISTING_SERVICE_HOST", value = "listing-service.safar.local" },
        { name = "BOOKING_SERVICE_HOST", value = "booking-service.safar.local" },
        { name = "PAYMENT_SERVICE_HOST", value = "payment-service.safar.local" },
        { name = "SEARCH_SERVICE_HOST", value = "search-service.safar.local" },
        { name = "REVIEW_SERVICE_HOST", value = "review-service.safar.local" },
        { name = "MEDIA_SERVICE_HOST", value = "media-service.safar.local" },
        { name = "NOTIFICATION_SERVICE_HOST", value = "notification-service.safar.local" },
        { name = "MESSAGING_SERVICE_HOST", value = "messaging-service.safar.local" },
        { name = "AI_SERVICE_HOST", value = "ai-service.safar.local" },
      ],
      # Database config (application-prod.yml uses DB_URL, DB_USERNAME)
      contains(keys(local.db_schemas), each.key) ? [
        { name = "DB_URL", value = "jdbc:postgresql://${aws_db_instance.main.address}:5432/safar?currentSchema=${local.db_schemas[each.key]}" },
        { name = "DB_USERNAME", value = "safar_admin" },
      ] : [],
      # Redis config (application-prod.yml uses REDIS_HOST)
      contains(["api-gateway", "auth-service", "booking-service"], each.key) ? [
        { name = "REDIS_HOST", value = aws_elasticache_replication_group.main.primary_endpoint_address },
        { name = "REDIS_PORT", value = "6379" },
      ] : [],
      # Kafka config (application-prod.yml uses KAFKA_BOOTSTRAP_SERVERS)
      contains(local.kafka_services, each.key) ? [
        { name = "KAFKA_BOOTSTRAP_SERVERS", value = "boot-2ege0pbm.c3.kafka-serverless.ap-south-1.amazonaws.com:9098" },
      ] : [],
      # Mail config for auth-service and notification-service
      contains(["auth-service", "notification-service"], each.key) ? [
        { name = "SPRING_MAIL_HOST", value = "smtp.gmail.com" },
        { name = "SPRING_MAIL_PORT", value = "587" },
        { name = "NOTIFICATION_FROM_EMAIL", value = "noreply@ysafar.com" },
      ] : [],
      # S3/CDN for media-service and listing-service
      contains(["media-service", "listing-service"], each.key) ? [
        { name = "S3_BUCKET", value = aws_s3_bucket.media.id },
        { name = "AWS_REGION", value = var.aws_region },
        { name = "CDN_DOMAIN", value = aws_cloudfront_distribution.media.domain_name },
      ] : [],
      # Elasticsearch for search-service
      each.key == "search-service" ? [
        { name = "ELASTICSEARCH_URI", value = "https://search-service.safar.local:9200" },
      ] : [],
    )

    secrets = concat(
      [
        { name = "JWT_SECRET", valueFrom = aws_secretsmanager_secret.jwt_secret.arn },
      ],
      # DB password (application-prod.yml uses DB_PASSWORD)
      contains(keys(local.db_schemas), each.key) ? [
        { name = "DB_PASSWORD", valueFrom = aws_secretsmanager_secret.db_password.arn },
      ] : [],
      each.key == "user-service" ? [
        { name = "ENCRYPTION_AES_KEY", valueFrom = aws_secretsmanager_secret.aes_key.arn },
      ] : [],
      # Razorpay for payment-service AND user-service (both use it)
      contains(["payment-service", "user-service"], each.key) ? [
        { name = "RAZORPAY_KEY_ID", valueFrom = "${aws_secretsmanager_secret.razorpay.arn}:key_id::" },
        { name = "RAZORPAY_KEY_SECRET", valueFrom = "${aws_secretsmanager_secret.razorpay.arn}:key_secret::" },
        { name = "RAZORPAY_WEBHOOK_SECRET", valueFrom = "${aws_secretsmanager_secret.razorpay.arn}:webhook_secret::" },
      ] : [],
      # Mail creds for auth-service and notification-service
      contains(["auth-service", "notification-service"], each.key) ? [
        { name = "SPRING_MAIL_USERNAME", valueFrom = "${aws_secretsmanager_secret.mail.arn}:username::" },
        { name = "SPRING_MAIL_PASSWORD", valueFrom = "${aws_secretsmanager_secret.mail.arn}:password::" },
      ] : [],
    )

    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = "/ecs/${var.project}/${each.key}"
        "awslogs-region"        = var.aws_region
        "awslogs-stream-prefix" = "ecs"
      }
    }

    # Health check
    healthCheck = {
      command     = ["CMD-SHELL", "curl -f http://localhost:${each.value.port}${each.value.health_path} || exit 1"]
      interval    = 30
      timeout     = 5
      retries     = 3
      startPeriod = 120
    }
  }])
}

# ── ECS Services ─────────────────────────────────────────────────────────────

resource "aws_ecs_service" "services" {
  for_each = var.services

  name            = each.key
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.services[each.key].arn
  desired_count   = each.value.desired_count
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = module.vpc.private_subnets
    security_groups  = [aws_security_group.ecs_tasks.id]
    assign_public_ip = false
  }

  # Only api-gateway is behind ALB
  dynamic "load_balancer" {
    for_each = each.key == "api-gateway" ? [1] : []
    content {
      target_group_arn = aws_lb_target_group.api_gateway.arn
      container_name   = each.key
      container_port   = each.value.port
    }
  }

  service_registries {
    registry_arn = aws_service_discovery_service.services[each.key].arn
  }

  deployment_circuit_breaker {
    enable   = true
    rollback = true
  }

  deployment_maximum_percent         = 200
  deployment_minimum_healthy_percent = 100

  lifecycle {
    ignore_changes = [desired_count] # Managed by auto-scaling
  }
}

# ── Auto Scaling ─────────────────────────────────────────────────────────────

resource "aws_appautoscaling_target" "services" {
  for_each = var.services

  max_capacity       = each.value.max_count
  min_capacity       = each.value.min_count
  resource_id        = "service/${aws_ecs_cluster.main.name}/${each.key}"
  scalable_dimension = "ecs:service:DesiredCount"
  service_namespace  = "ecs"

  depends_on = [aws_ecs_service.services]
}

resource "aws_appautoscaling_policy" "cpu" {
  for_each = var.services

  name               = "${each.key}-cpu-scaling"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.services[each.key].resource_id
  scalable_dimension = aws_appautoscaling_target.services[each.key].scalable_dimension
  service_namespace  = aws_appautoscaling_target.services[each.key].service_namespace

  target_tracking_scaling_policy_configuration {
    target_value       = 70.0
    scale_in_cooldown  = 300
    scale_out_cooldown = 60

    predefined_metric_specification {
      predefined_metric_type = "ECSServiceAverageCPUUtilization"
    }
  }
}
