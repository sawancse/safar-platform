# GitHub Secrets & Variables Setup for Safar CI/CD

## Required Secrets

Go to **GitHub repo → Settings → Secrets and variables → Actions → New repository secret** and add these:

---

### 1. `AWS_DEPLOY_ROLE_ARN`

An **IAM Role ARN** that GitHub Actions assumes via OIDC (OpenID Connect) to deploy to AWS — no long-lived access keys needed.

**Example value:** `arn:aws:iam::123456789012:role/sawancse`
arn:aws:iam::624627192872:oidc-provider/token.actions.githubusercontent.com
#### How to create it:

```bash
# Step 1: Create an OIDC identity provider for GitHub in your AWS account
aws iam create-open-id-connect-provider \
  --url "https://token.actions.githubusercontent.com" \
  --client-id-list "sts.amazonaws.com" \
  --thumbprint-list "6938fd4d98bab03faadb97b34396831e3780aea1"

# Step 2: Create an IAM role that GitHub can assume
# Replace 624627192872 and <YOUR_ORG> with your actual values
aws iam create-role --role-name sawancse \
  --assume-role-policy-document '{
    "Version": "2012-10-17",
    "Statement": [{
      "Effect": "Allow",
      "Principal": {
        "Federated": "arn:aws:iam::624627192872:oidc-provider/token.actions.githubusercontent.com"
      },
      "Action": "sts:AssumeRoleWithWebIdentity",
      "Condition": {
        "StringEquals": {
          "token.actions.githubusercontent.com:aud": "sts.amazonaws.com"
        },
        "StringLike": {
          "token.actions.githubusercontent.com:sub": "repo:sawancse/safar-platform:*"
        }
      }
    }]
  }'

# Step 3: Attach ECR permissions (push Docker images)
aws iam attach-role-policy --role-name sawancse \
  --policy-arn arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryPowerUser

# Step 4: Create and attach custom policy for ECS, S3, CloudFront
aws iam put-role-policy --role-name sawancse \
  --policy-name safar-deploy-policy \
  --policy-document '{
    "Version": "2012-10-17",
    "Statement": [
      {
        "Sid": "ECSDeployment",
        "Effect": "Allow",
        "Action": [
          "ecs:UpdateService",
          "ecs:DescribeServices",
          "ecs:DescribeTaskDefinition",
          "ecs:RegisterTaskDefinition",
          "ecs:ListServices",
          "iam:PassRole"
        ],
        "Resource": "*"
      },
      {
        "Sid": "S3AdminDashboard",
        "Effect": "Allow",
        "Action": [
          "s3:PutObject",
          "s3:GetObject",
          "s3:DeleteObject",
          "s3:ListBucket"
        ],
        "Resource": [
          "arn:aws:s3:::arn:aws:s3:::safar-media-dev",
          "arn:aws:s3:::safar-admin-production/*"
        ]
      },
      {
        "Sid": "CloudFrontInvalidation",
        "Effect": "Allow",
        "Action": [
          "cloudfront:CreateInvalidation",
          "cloudfront:GetInvalidation"
        ],
        "Resource": "*"
      }
    ]
  }'
```

The resulting ARN to store as the secret: `arn:aws:iam::<YOUR_ACCOUNT_ID>:role/sawancse`

---

### 2. `ADMIN_CLOUDFRONT_DIST_ID`

The **CloudFront Distribution ID** for the admin dashboard. Used by `deploy-admin.yml` to invalidate the cache after deploying new static files.

**Example value:** `E1A2B3C4D5E6F7`

#### How to get it:

After running `terraform apply`:

```bash
# Option 1: From Terraform output
cd infrastructure/terraform
terraform output admin_cdn_domain

# Option 2: From AWS CLI
aws cloudfront list-distributions \
  --query "DistributionList.Items[?Comment=='Safar admin dashboard'].Id" \
  --output text
```

---

## Required Variables

Go to **GitHub repo → Settings → Secrets and variables → Actions → Variables tab → New repository variable**:

| Variable | Example Value | Used By |
|----------|---------------|---------|
| `NEXT_PUBLIC_API_URL` | `https://api.safar.com` | `deploy-web.yml` — Next.js API base URL |
| `NEXT_PUBLIC_CDN_DOMAIN` | `media.safar.com` | `deploy-web.yml` — Media CDN domain for images |
| `VITE_API_URL` | `https://api.safar.com` | `deploy-admin.yml` — Admin dashboard API URL |

---

## Verification

After setting everything up, you can verify the OIDC connection works by manually triggering a workflow:

```bash
# Trigger the CI workflow
gh workflow run ci.yml

# Or trigger a specific deploy
gh workflow run deploy-services.yml -f service=auth-service
```

Check the workflow run in **GitHub → Actions** tab to confirm AWS credentials are configured correctly.

---

## Security Notes

- **OIDC is preferred over access keys** — no long-lived credentials stored in GitHub
- The role trust policy restricts access to your specific GitHub repo only (`repo:<YOUR_ORG>/safar-platform:*`)
- The deploy policy follows **least privilege** — only ECS deploy, ECR push, S3 sync, and CloudFront invalidation
- Secrets are never logged in GitHub Actions output (automatically masked)
