package com.safar.listing.dto;

import com.safar.listing.entity.enums.MaterialCategory;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MaterialCatalogResponse(
        UUID id,
        MaterialCategory category,
        String name,
        String brand,
        String description,
        String imageUrl,
        Long unitPricePaise,
        String unit,
        String specifications,
        Integer warrantyYears,
        Boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
