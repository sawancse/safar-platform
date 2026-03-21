# ── S3 Buckets ───────────────────────────────────────────────────────────────

# Media bucket (property photos, KYC docs, etc.)
resource "aws_s3_bucket" "media" {
  bucket = "${var.project}-media-${var.environment}"
}

resource "aws_s3_bucket_versioning" "media" {
  bucket = aws_s3_bucket.media.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "media" {
  bucket = aws_s3_bucket.media.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_intelligent_tiering_configuration" "media" {
  bucket = aws_s3_bucket.media.id
  name   = "auto-tier"

  tiering {
    access_tier = "ARCHIVE_ACCESS"
    days        = 90
  }
}

resource "aws_s3_bucket_cors_configuration" "media" {
  bucket = aws_s3_bucket.media.id

  cors_rule {
    allowed_headers = ["*"]
    allowed_methods = ["GET", "PUT", "POST"]
    allowed_origins = ["https://${var.domain_name}", "https://www.${var.domain_name}"]
    max_age_seconds = 3600
  }
}

resource "aws_s3_bucket_public_access_block" "media" {
  bucket = aws_s3_bucket.media.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# Admin dashboard static hosting bucket
resource "aws_s3_bucket" "admin" {
  bucket = "${var.project}-admin-${var.environment}"
}

resource "aws_s3_bucket_website_configuration" "admin" {
  bucket = aws_s3_bucket.admin.id

  index_document { suffix = "index.html" }
  error_document { key = "index.html" } # SPA fallback
}

resource "aws_s3_bucket_public_access_block" "admin" {
  bucket = aws_s3_bucket.admin.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# Terraform state bucket (create manually first time)
# resource "aws_s3_bucket" "terraform_state" {
#   bucket = "safar-terraform-state"
# }
