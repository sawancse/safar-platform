package com.safar.listing.dto;

import com.safar.listing.entity.enums.ExperienceCategory;
import com.safar.listing.entity.enums.ExperienceStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
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
        Integer groupDiscountPct,
        ExperienceStatus status,
        BigDecimal avgRating,
        int reviewCount,
        String hostName,
        List<SessionResponse> upcomingSessions,
        OffsetDateTime createdAt
) {}
