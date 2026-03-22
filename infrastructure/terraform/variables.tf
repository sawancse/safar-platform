variable "aws_region" {
  description = "AWS region for all resources"
  type        = string
  default     = "ap-south-1" # Mumbai — lowest latency for India
}

variable "project" {
  description = "Project name used for tagging and naming"
  type        = string
  default     = "safar"
}

variable "environment" {
  description = "Environment name (dev, staging, production)"
  type        = string
  default     = "production"
}

variable "domain_name" {
  description = "Primary domain name"
  type        = string
  default     = "ysafar.com"
}

variable "db_password" {
  description = "RDS master password"
  type        = string
  sensitive   = true
}

variable "jwt_secret" {
  description = "JWT signing secret (base64-encoded)"
  type        = string
  sensitive   = true
}

variable "aes_encryption_key" {
  description = "AES-256 key for PII encryption"
  type        = string
  sensitive   = true
}

variable "razorpay_key_id" {
  description = "Razorpay key ID"
  type        = string
  sensitive   = true
}

variable "razorpay_key_secret" {
  description = "Razorpay key secret"
  type        = string
  sensitive   = true
}

variable "razorpay_webhook_secret" {
  description = "Razorpay webhook secret"
  type        = string
  sensitive   = true
}

variable "mail_username" {
  description = "SMTP email username"
  type        = string
  default     = ""
}

variable "mail_password" {
  description = "SMTP email password"
  type        = string
  sensitive   = true
  default     = ""
}

# ── Service sizing ───────────────────────────────────────────────────────────

variable "services" {
  description = "ECS service configurations"
  type = map(object({
    port          = number
    cpu           = number
    memory        = number
    desired_count = number
    min_count     = number
    max_count     = number
    health_path   = string
  }))
  # MVP sizing — all services single instance, minimal resources
  # Scale up for prod: increase desired_count, cpu, memory
  default = {
    api-gateway = {
      port = 8080, cpu = 256, memory = 512,
      desired_count = 1, min_count = 1, max_count = 4,
      health_path = "/actuator/health"
    }
    auth-service = {
      port = 8888, cpu = 256, memory = 512,
      desired_count = 1, min_count = 1, max_count = 3,
      health_path = "/actuator/health"
    }
    user-service = {
      port = 8092, cpu = 256, memory = 512,
      desired_count = 1, min_count = 1, max_count = 2,
      health_path = "/actuator/health"
    }
    listing-service = {
      port = 8083, cpu = 256, memory = 512,
      desired_count = 1, min_count = 1, max_count = 3,
      health_path = "/actuator/health"
    }
    search-service = {
      port = 8084, cpu = 256, memory = 512,
      desired_count = 1, min_count = 1, max_count = 4,
      health_path = "/actuator/health"
    }
    booking-service = {
      port = 8095, cpu = 256, memory = 512,
      desired_count = 1, min_count = 1, max_count = 3,
      health_path = "/actuator/health"
    }
    payment-service = {
      port = 8086, cpu = 256, memory = 512,
      desired_count = 1, min_count = 1, max_count = 2,
      health_path = "/actuator/health"
    }
    review-service = {
      port = 8087, cpu = 256, memory = 512,
      desired_count = 1, min_count = 1, max_count = 2,
      health_path = "/actuator/health"
    }
    media-service = {
      port = 8088, cpu = 256, memory = 512,
      desired_count = 1, min_count = 1, max_count = 2,
      health_path = "/actuator/health"
    }
    notification-service = {
      port = 8089, cpu = 256, memory = 512,
      desired_count = 1, min_count = 1, max_count = 2,
      health_path = "/actuator/health"
    }
    messaging-service = {
      port = 8091, cpu = 256, memory = 512,
      desired_count = 1, min_count = 1, max_count = 2,
      health_path = "/actuator/health"
    }
    ai-service = {
      port = 8090, cpu = 256, memory = 512,
      desired_count = 1, min_count = 1, max_count = 2,
      health_path = "/health"
    }
  }
}
