package com.safar.media.controller;

import com.safar.media.dto.ConfirmUploadRequest;
import com.safar.media.dto.PresignResponse;
import com.safar.media.service.MediaService;
import com.safar.media.service.S3Gateway;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;
    private final S3Gateway s3Gateway;

    @PostMapping("/upload/presign")
    public ResponseEntity<PresignResponse> presign(
            @RequestParam String mediaType,
            @RequestParam String contentType,
            @RequestParam UUID listingId) {
        PresignResponse resp = mediaService.generatePresignedUrl(mediaType, contentType, listingId);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/upload/confirm")
    public ResponseEntity<Void> confirm(@Valid @RequestBody ConfirmUploadRequest request) {
        mediaService.confirmUpload(request);
        return ResponseEntity.ok().build();
    }

    /** Presign upload for KYC documents (Aadhaar, PAN, Selfie) */
    @PostMapping("/upload/kyc-presign")
    public ResponseEntity<Map<String, String>> kycPresign(
            @RequestParam String docType,
            @RequestParam String contentType,
            @RequestHeader("X-User-Id") UUID userId) {
        String key = "kyc/" + userId + "/" + docType + "-" + System.currentTimeMillis()
                + "." + contentType.split("/")[1];
        String uploadUrl = s3Gateway.generatePresignedUrl(key, contentType);
        String publicUrl = uploadUrl.split("\\?")[0];
        return ResponseEntity.ok(Map.of("uploadUrl", uploadUrl, "publicUrl", publicUrl, "key", key));
    }

    /** Presign upload for user avatar / profile picture */
    @PostMapping("/upload/avatar-presign")
    public ResponseEntity<Map<String, String>> avatarPresign(
            @RequestParam String contentType,
            @RequestHeader("X-User-Id") UUID userId) {
        String ext = contentType.contains("/") ? contentType.split("/")[1] : "jpg";
        String key = "avatars/" + userId + "/avatar-" + System.currentTimeMillis() + "." + ext;
        String uploadUrl = s3Gateway.generatePresignedUrl(key, contentType);
        String publicUrl = uploadUrl.split("\\?")[0];
        return ResponseEntity.ok(Map.of("uploadUrl", uploadUrl, "publicUrl", publicUrl, "key", key));
    }

    /** Generic presign for builder projects, sale properties, etc. */
    @PostMapping("/upload/generic-presign")
    public ResponseEntity<Map<String, String>> genericPresign(
            @RequestParam String folder,
            @RequestParam String contentType,
            @RequestHeader("X-User-Id") UUID userId) {
        String ext = contentType.contains("/") ? contentType.split("/")[1] : "jpg";
        String key = folder + "/" + userId + "/" + System.currentTimeMillis() + "." + ext;
        String uploadUrl = s3Gateway.generatePresignedUrl(key, contentType);
        String publicUrl = uploadUrl.split("\\?")[0];
        return ResponseEntity.ok(Map.of("uploadUrl", uploadUrl, "publicUrl", publicUrl, "key", key));
    }
}
