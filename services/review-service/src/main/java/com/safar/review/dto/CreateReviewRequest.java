package com.safar.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CreateReviewRequest(
        @NotNull UUID bookingId,
        @NotNull @Min(1) @Max(5) Short rating,
        String comment,
        List<String> guestPhotoUrls,
        @Min(1) @Max(5) Short ratingCleanliness,
        @Min(1) @Max(5) Short ratingLocation,
        @Min(1) @Max(5) Short ratingValue,
        @Min(1) @Max(5) Short ratingCommunication,
        @Min(1) @Max(5) Short ratingCheckIn,
        @Min(1) @Max(5) Short ratingAccuracy,
        @Min(1) @Max(5) Short ratingStaff,
        @Min(1) @Max(5) Short ratingFacilities,
        @Min(1) @Max(5) Short ratingComfort,
        @Min(1) @Max(5) Short ratingFreeWifi,
        // Per-category text comments as JSON string: {"cleanliness":"...", "location":"..."}
        String categoryComments
) {}
