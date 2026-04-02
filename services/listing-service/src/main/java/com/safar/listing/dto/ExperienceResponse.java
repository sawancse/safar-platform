package com.safar.listing.dto;

import com.safar.listing.entity.enums.ExperienceCategory;
import com.safar.listing.entity.enums.ExperienceStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ExperienceResponse(
        UUID id,
        UUID hostId,
        String title,
        String description,
        ExperienceCategory category,
        String city,
        String locationName,
        int durationMinutes,
        int maxGuests,
        long pricePaise,
        String languagesSpoken,
        String mediaUrls,
        ExperienceStatus status,
        BigDecimal avgRating,
        int reviewCount,
        String hostName,
        OffsetDateTime createdAt
) {}
