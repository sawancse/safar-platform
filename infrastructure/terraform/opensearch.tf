# ── Amazon OpenSearch (Elasticsearch compatible) ─────────────────────────────
# TODO: Enable after activating OpenSearch service in AWS account
#       (SubscriptionRequiredException)

# resource "aws_opensearch_domain" "main" {
#   domain_name    = "${var.project}-search"
#   engine_version = "OpenSearch_2.11"
#
#   cluster_config {
#     instance_type          = "t3.medium.search"
#     instance_count         = 2
#     zone_awareness_enabled = true
#
#     zone_awareness_config {
#       availability_zone_count = 2
#     }
#   }
#
#   ebs_options {
#     ebs_enabled = true
#     volume_size = 30
#     volume_type = "gp3"
#   }
#
#   vpc_options {
#     subnet_ids         = module.vpc.private_subnets
#     security_group_ids = [aws_security_group.opensearch.id]
#   }
#
#   encrypt_at_rest {
#     enabled = true
#   }
#
#   node_to_node_encryption {
#     enabled = true
#   }
#
#   domain_endpoint_options {
#     enforce_https       = true
#     tls_security_policy = "Policy-Min-TLS-1-2-PFS-2023-10"
#   }
#
#   advanced_options = {
#     "rest.action.multi.allow_explicit_index" = "true"
#   }
#
#   access_policies = jsonencode({
#     Version = "2012-10-17"
#     Statement = [{
#       Effect    = "Allow"
#       Principal = { AWS = aws_iam_role.ecs_task.arn }
#       Action    = "es:*"
#       Resource  = "arn:aws:es:${var.aws_region}:*:domain/${var.project}-search/*"
#     }]
#   })
# }
