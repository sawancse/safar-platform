package com.safar.listing.dto;

import com.safar.listing.entity.enums.ExperienceCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ExperienceRequest(
        @NotBlank String title,
        @NotBlank String description,
        @NotNull ExperienceCategory category,
        @NotBlank String city,
        String locationName,
        @NotNull BigDecimal durationHours,
        Integer maxGuests,
        @Positive long pricePaise,
        String languagesSpoken,
        String mediaUrls,
        String whatsIncluded,
        String whatsNotIncluded,
        String itinerary,
        String meetingPoint,
        BigDecimal meetingPointLat,
        BigDecimal meetingPointLng,
        String accessibility,
        String cancellationPolicy,
        Integer minAge,
        Boolean isPrivate,
        Integer groupDiscountPct
) {}
