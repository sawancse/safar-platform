package com.safar.media.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ConfirmUploadRequest(
        @NotNull UUID mediaId,
        @NotNull UUID listingId,
        @NotBlank String s3Key,
        @NotBlank String mediaType,
        int durationSeconds
) {}
