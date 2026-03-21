package com.safar.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SubmitVideoReviewRequest(
        @NotNull UUID listingId,
        @NotBlank String s3Key,
        String cdnUrl,
        @NotNull Integer durationSeconds
) {}
