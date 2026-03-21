# S3 Photo Upload Setup

## Environment Variables

Set these before starting listing-service and media-service:

```bash
export AWS_ACCESS_KEY_ID=your-access-key
export AWS_SECRET_ACCESS_KEY=your-secret-key
export S3_BUCKET=safar-media-dev
export AWS_REGION=ap-south-1
export CDN_DOMAIN=your-cloudfront-domain.cloudfront.net
```

## S3 Bucket CORS Configuration

Enable CORS on your S3 bucket for browser direct uploads (presign flow):

```json
[
  {
    "AllowedHeaders": ["*"],
    "AllowedMethods": ["PUT"],
    "AllowedOrigins": ["http://localhost:3000"],
    "ExposeHeaders": ["ETag"]
  }
]
```

For production, replace `http://localhost:3000` with your actual frontend domain.

## AWS CLI Commands

### Create bucket
```bash
aws s3 mb s3://safar-media-dev --region ap-south-1
```

### Apply CORS
```bash
aws s3api put-bucket-cors --bucket safar-media-dev --cors-configuration file://s3-cors.json
```

### Create CloudFront distribution (optional for dev)
If you don't have CloudFront, you can use the S3 public URL directly by making the bucket publicly readable or using presigned GET URLs.

## Upload Flows

### Flow 1: Host Dashboard (listing-service direct upload)
```
Frontend → POST /api/v1/listings/{id}/media/upload (multipart)
         → listing-service uploads to S3 via S3StorageService
         → Returns CDN URL: https://{CDN_DOMAIN}/listings/{id}/photo/{uuid}.jpg
```

### Flow 2: PhotoManager (media-service presigned URL)
```
Frontend → POST /api/v1/media/upload/presign
         → media-service returns presigned S3 PUT URL (15 min validity)
Frontend → PUT directly to S3 presigned URL
Frontend → POST /api/v1/media/upload/confirm
         → media-service publishes Kafka event → listing-service saves record
```

## S3 Key Structure
```
listings/{listingId}/photo/{uuid}.jpg
listings/{listingId}/video/{uuid}.mp4
listings/{listingId}/panorama/{uuid}.jpg
listings/{listingId}/floor_plan/{uuid}.pdf
```
