package com.safar.listing.dto;

import java.util.UUID;

public record MaterialSelectionRequest(
        UUID roomDesignId,
        UUID materialId,
        String category,
        String materialName,
        String brand,
        Integer quantity,
        Long unitPricePaise
) {}
