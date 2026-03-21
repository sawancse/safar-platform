terraform {
  required_version = ">= 1.5.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.40"
    }
  }

  # Uncomment after first apply to migrate state to S3
  # backend "s3" {
  #   bucket         = "safar-terraform-state"
  #   key            = "production/terraform.tfstate"
  #   region         = "ap-south-1"
  #   dynamodb_table = "safar-terraform-lock"
  #   encrypt        = true
  # }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = var.project
      Environment = var.environment
      ManagedBy   = "terraform"
    }
  }
}

# For CloudFront ACM certificate (must be us-east-1)
provider "aws" {
  alias  = "us_east_1"
  region = "us-east-1"
}

locals {
  name_prefix = "${var.project}-${var.environment}"

  # Services that need PostgreSQL
  db_services = toset([
    "auth-service", "user-service", "listing-service",
    "booking-service", "payment-service", "review-service",
    "messaging-service"
  ])

  # Services that need Kafka
  kafka_services = toset([
    "listing-service", "booking-service", "payment-service",
    "media-service", "search-service", "review-service",
    "notification-service", "messaging-service", "user-service"
  ])

  # Schema mapping per service
  db_schemas = {
    "auth-service"         = "auth"
    "user-service"         = "users"
    "listing-service"      = "listings"
    "booking-service"      = "bookings"
    "payment-service"      = "payments"
    "review-service"       = "reviews"
    "messaging-service"    = "messages"
  }
}
