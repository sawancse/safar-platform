# ── AWS Secrets Manager ──────────────────────────────────────────────────────

resource "aws_secretsmanager_secret" "db_password" {
  name = "${local.name_prefix}/db-password"
}

resource "aws_secretsmanager_secret_version" "db_password" {
  secret_id     = aws_secretsmanager_secret.db_password.id
  secret_string = var.db_password
}

resource "aws_secretsmanager_secret" "jwt_secret" {
  name = "${local.name_prefix}/jwt-secret"
}

resource "aws_secretsmanager_secret_version" "jwt_secret" {
  secret_id     = aws_secretsmanager_secret.jwt_secret.id
  secret_string = var.jwt_secret
}

resource "aws_secretsmanager_secret" "aes_key" {
  name = "${local.name_prefix}/aes-encryption-key"
}

resource "aws_secretsmanager_secret_version" "aes_key" {
  secret_id     = aws_secretsmanager_secret.aes_key.id
  secret_string = var.aes_encryption_key
}

resource "aws_secretsmanager_secret" "razorpay" {
  name = "${local.name_prefix}/razorpay"
}

resource "aws_secretsmanager_secret_version" "razorpay" {
  secret_id = aws_secretsmanager_secret.razorpay.id
  secret_string = jsonencode({
    key_id         = var.razorpay_key_id
    key_secret     = var.razorpay_key_secret
    webhook_secret = var.razorpay_webhook_secret
  })
}

resource "aws_secretsmanager_secret" "mail" {
  name = "${local.name_prefix}/mail-credentials"
}

resource "aws_secretsmanager_secret_version" "mail" {
  secret_id = aws_secretsmanager_secret.mail.id
  secret_string = jsonencode({
    username = var.mail_username
    password = var.mail_password
  })
}
