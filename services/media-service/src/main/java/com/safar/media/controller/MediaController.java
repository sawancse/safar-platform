package com.safar.media.controller;

import com.safar.media.dto.ConfirmUploadRequest;
import com.safar.media.dto.PresignResponse;
import com.safar.media.service.MediaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.UUID;

@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;

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
}
