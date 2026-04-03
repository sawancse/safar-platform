package com.safar.listing.dto;

import com.safar.listing.entity.enums.FinancingType;
import com.safar.listing.entity.enums.InquiryStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record InquiryResponse(
        UUID id,
        UUID salePropertyId,
        UUID buyerId,
        UUID sellerId,
        InquiryStatus status,
        String message,
        String buyerName,
        String buyerPhone,
        String buyerEmail,
        LocalDate preferredVisitDate,
        String preferredVisitTime,
        FinancingType financingType,
        Long budgetMinPaise,
        Long budgetMaxPaise,
        String notes,
        // Denormalized property info for list views
        String propertyTitle,
        String propertyLocality,
        String propertyCity,
        Long propertyPricePaise,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
