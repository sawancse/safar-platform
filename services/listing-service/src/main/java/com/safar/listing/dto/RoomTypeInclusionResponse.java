package com.safar.listing.dto;

import java.util.UUID;

public record RoomTypeInclusionResponse(
        UUID id,
        UUID roomTypeId,
        String category,
        String name,
        String description,
        String inclusionMode,
        Long chargePaise,
        String chargeType,
        Integer discountPercent,
        String terms,
        Boolean isHighlight,
        Integer sortOrder,
        Boolean isActive
) {}
