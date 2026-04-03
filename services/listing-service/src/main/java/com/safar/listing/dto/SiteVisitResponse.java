package com.safar.listing.dto;

import com.safar.listing.entity.enums.VisitStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SiteVisitResponse(
        UUID id,
        UUID inquiryId,
        UUID salePropertyId,
        UUID buyerId,
        UUID sellerId,
        OffsetDateTime scheduledAt,
        Integer durationMinutes,
        VisitStatus status,
        String buyerFeedback,
        String sellerFeedback,
        Integer rating,
        // Denormalized
        String propertyTitle,
        String propertyLocality,
        String propertyCity,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
