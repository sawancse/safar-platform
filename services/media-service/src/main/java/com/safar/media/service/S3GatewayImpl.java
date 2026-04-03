package com.safar.media.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;

@Component
public class S3GatewayImpl implements S3Gateway {

    private final S3Presigner presigner;
    private final S3Client s3Client;
    private final String bucket;

    public S3GatewayImpl(
            @Value("${aws.s3.region}") String region,
            @Value("${aws.s3.access-key:}") String accessKey,
            @Value("${aws.s3.secret-key:}") String secretKey,
            @Value("${aws.s3.bucket}") String bucket) {
        AwsCredentialsProvider credentials;
        if (accessKey != null && !accessKey.isBlank() && secretKey != null && !secretKey.isBlank()) {
            credentials = StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKey, secretKey));
        } else {
            credentials = DefaultCredentialsProvider.create();
        }
        this.bucket = bucket;
        this.presigner = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(credentials)
                .build();
        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(credentials)
                .build();
    }

    @Override
    public String generatePresignedUrl(String s3Key, String contentType) {
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(s3Key)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(15))
                .putObjectRequest(objectRequest)
                .build();

        PresignedPutObjectRequest presigned = presigner.presignPutObject(presignRequest);
        return presigned.url().toString();
    }

    @Override
    public byte[] download(String s3Key) {
        return s3Client.getObjectAsBytes(
                GetObjectRequest.builder().bucket(bucket).key(s3Key).build()
        ).asByteArray();
    }

    @Override
    public void upload(String s3Key, byte[] data, String contentType) {
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(s3Key)
                        .contentType(contentType)
                        .cacheControl("public, max-age=31536000")
                        .build(),
                RequestBody.fromBytes(data)
        );
    }
}
