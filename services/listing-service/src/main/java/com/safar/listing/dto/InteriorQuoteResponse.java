package com.safar.listing.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record InteriorQuoteResponse(
        UUID id,
        UUID projectId,
        Integer version,
        Long materialCostPaise,
        Long labourCostPaise,
        Long overheadPaise,
        Long taxPaise,
        Long discountPaise,
        Long totalPaise,
        String status,
        String remarks,
        String quoteDocumentUrl,
        OffsetDateTime validUntil,
        OffsetDateTime approvedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
