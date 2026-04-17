package com.safar.listing.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Service
@Slf4j
public class S3StorageService {

    private final S3Client s3Client;
    private final String bucket;
    private final String cdnDomain;
    private final boolean localMode;

    public S3StorageService(
            @Value("${aws.s3.bucket}") String bucket,
            @Value("${aws.s3.region}") String region,
            @Value("${aws.s3.access-key:}") String accessKey,
            @Value("${aws.s3.secret-key:}") String secretKey,
            @Value("${aws.cloudfront.domain}") String cdnDomain,
            @Value("${storage.local-mode:false}") boolean localMode) {
        this.bucket = bucket;
        this.cdnDomain = cdnDomain;
        this.localMode = localMode;

        if (localMode || accessKey == null || accessKey.isBlank() || secretKey == null || secretKey.isBlank()) {
            this.s3Client = null;
            log.info("S3 storage in LOCAL MODE — files saved to ./uploads/");
        } else {
            this.s3Client = S3Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKey, secretKey)))
                    .build();
        }
    }

    /**
     * Uploads a file to S3 (or local disk in local mode) and returns the URL.
     */
    public String upload(UUID listingId, MultipartFile file, String mediaType) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }
        String s3Key = "listings/" + listingId + "/" + mediaType.toLowerCase() + "/" + UUID.randomUUID() + ext;

        if (s3Client == null) {
            // Local mode: save to disk (absolute path to avoid Tomcat temp dir issues)
            java.nio.file.Path uploadDir = java.nio.file.Paths.get(System.getProperty("user.dir"),
                    "uploads", "listings", listingId.toString(), mediaType.toLowerCase()).toAbsolutePath();
            java.nio.file.Files.createDirectories(uploadDir);
            java.nio.file.Path target = uploadDir.resolve(s3Key.substring(s3Key.lastIndexOf('/') + 1));
            try (var in = file.getInputStream(); var out = java.nio.file.Files.newOutputStream(target)) {
                in.transferTo(out);
            }
            String localUrl = "http://localhost:8083/uploads/" + s3Key;
            log.info("Local upload: {} → {}", originalFilename, target);
            return localUrl;
        }

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(s3Key)
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(putRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        log.info("Uploaded {} to S3: {}", originalFilename, s3Key);

        return "https://" + cdnDomain + "/" + s3Key;
    }

    /**
     * Deletes a file from S3 by its key.
     */
    public void delete(String s3Key) {
        if (s3Client == null) {
            log.warn("S3 not configured — skipping delete for {}", s3Key);
            return;
        }
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Key)
                    .build());
            log.info("Deleted from S3: {}", s3Key);
        } catch (Exception e) {
            log.warn("Failed to delete S3 object {}: {}", s3Key, e.getMessage());
        }
    }

    /**
     * Extracts the S3 key from a CDN URL.
     */
    public String extractS3Key(String cdnUrl) {
        String prefix = "https://" + cdnDomain + "/";
        if (cdnUrl != null && cdnUrl.startsWith(prefix)) {
            return cdnUrl.substring(prefix.length());
        }
        return null;
    }
}
