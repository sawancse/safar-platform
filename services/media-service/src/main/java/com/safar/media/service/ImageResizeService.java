package com.safar.media.service;

import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

@Service
@Slf4j
public class ImageResizeService {

    private final S3Gateway s3Gateway;
    private final boolean enabled;
    private final int width;
    private final int height;
    private final double quality;
    private final long maxOriginalBytes;

    public ImageResizeService(
            S3Gateway s3Gateway,
            @Value("${media.resize.enabled:true}") boolean enabled,
            @Value("${media.resize.width:720}") int width,
            @Value("${media.resize.height:480}") int height,
            @Value("${media.resize.quality:0.85}") double quality,
            @Value("${media.resize.max-original-bytes:15728640}") long maxOriginalBytes) {
        this.s3Gateway = s3Gateway;
        this.enabled = enabled;
        this.width = width;
        this.height = height;
        this.quality = quality;
        this.maxOriginalBytes = maxOriginalBytes;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Downloads the original image from S3, generates a single resized variant,
     * uploads it back to S3, and returns the variant CDN URL.
     * Returns null if resize fails or is skipped.
     */
    public String resize(String originalS3Key, String cdnDomain) {
        if (!enabled) {
            return null;
        }

        try {
            byte[] original = s3Gateway.download(originalS3Key);

            if (original.length > maxOriginalBytes) {
                log.warn("Image {} exceeds max size ({} bytes), skipping resize", originalS3Key, original.length);
                return null;
            }

            String basePath = originalS3Key.contains(".")
                    ? originalS3Key.substring(0, originalS3Key.lastIndexOf('.'))
                    : originalS3Key;
            String extension = originalS3Key.contains(".")
                    ? originalS3Key.substring(originalS3Key.lastIndexOf('.'))
                    : ".jpg";

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Thumbnails.of(new ByteArrayInputStream(original))
                    .size(width, height)
                    .keepAspectRatio(true)
                    .outputQuality(quality)
                    .outputFormat("jpg")
                    .toOutputStream(out);

            byte[] resized = out.toByteArray();
            String variantKey = basePath + "_thumb" + extension;
            s3Gateway.upload(variantKey, resized, "image/jpeg");

            String variantUrl = "https://" + cdnDomain + "/" + variantKey;
            log.info("Resized {} → {}x{} ({} bytes)", originalS3Key, width, height, resized.length);
            return variantUrl;
        } catch (Exception e) {
            log.error("Failed to resize {}: {}", originalS3Key, e.getMessage());
            return null;
        }
    }
}
