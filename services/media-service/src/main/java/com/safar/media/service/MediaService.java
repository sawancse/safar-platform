package com.safar.media.service;

import com.safar.media.dto.ConfirmUploadRequest;
import com.safar.media.dto.PresignResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaService {

    private final S3Gateway s3Gateway;
    private final ImageResizeService imageResizeService;
    private final KafkaTemplate<String, String> kafka;

    @Value("${aws.cloudfront.domain}")
    private String cdnDomain;

    @Value("${aws.s3.bucket}")
    private String bucket;

    public PresignResponse generatePresignedUrl(String mediaType, String contentType, UUID listingId) {
        UUID mediaId = UUID.randomUUID();
        String s3Key = "listings/" + listingId + "/" + mediaType.toLowerCase() + "/" + mediaId;

        String uploadUrl = s3Gateway.generatePresignedUrl(s3Key, contentType);
        String cdnUrl = "https://" + cdnDomain + "/" + s3Key;
        return new PresignResponse(mediaId, uploadUrl, s3Key, cdnUrl);
    }

    public void confirmUpload(ConfirmUploadRequest request) {
        if ("VIDEO".equalsIgnoreCase(request.mediaType()) && request.durationSeconds() < 60) {
            throw new IllegalArgumentException("Listing videos must be at least 60 seconds");
        }

        // AI moderation stub — in production: AWS Rekognition
        String moderationStatus = "APPROVED";

        String cdnUrl = "https://" + cdnDomain + "/" + request.s3Key();

        // Generate resized thumbnail for photos
        String thumbUrl = "";
        if ("PHOTO".equalsIgnoreCase(request.mediaType()) && imageResizeService.isEnabled()) {
            String result = imageResizeService.resize(request.s3Key(), cdnDomain);
            if (result != null) {
                thumbUrl = result;
            }
        }

        String event = """
                {"mediaId":"%s","listingId":"%s","s3Key":"%s","type":"%s","moderationStatus":"%s","cdnUrl":"%s","thumbUrl":"%s"}
                """.formatted(
                request.mediaId(), request.listingId(),
                request.s3Key(), request.mediaType(), moderationStatus, cdnUrl, thumbUrl);

        kafka.send("media.uploaded", request.listingId().toString(), event.strip());
        log.info("Media {} confirmed and moderated: {} (resize={})", request.mediaId(), moderationStatus,
                imageResizeService.isEnabled() && "PHOTO".equalsIgnoreCase(request.mediaType()));
    }
}
