package com.safar.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CreateExperienceReviewRequest(
        @NotNull UUID experienceBookingId,
        @NotNull UUID experienceId,
        @NotNull UUID hostId,
        @NotNull @Min(1) @Max(5) Short rating,
        String comment,
        List<String> guestPhotoUrls,
        @Min(1) @Max(5) Short ratingValue,
        @Min(1) @Max(5) Short ratingCommunication,
        @Min(1) @Max(5) Short ratingAccuracy
) {}
