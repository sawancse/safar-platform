# ── Amazon MSK Serverless (Kafka) ────────────────────────────────────────────

resource "aws_msk_serverless_cluster" "main" {
  cluster_name = "${local.name_prefix}-kafka"

  vpc_config {
    subnet_ids         = module.vpc.private_subnets
    security_group_ids = [aws_security_group.msk.id]
  }

  client_authentication {
    sasl {
      iam {
        enabled = true
      }
    }
  }
}
