package com.safar.listing.dto;

import java.util.UUID;

public record PgPackageResponse(
        UUID id,
        UUID listingId,
        String name,
        String description,
        Long monthlyPricePaise,
        Boolean includesMeals,
        Boolean includesLaundry,
        Boolean includesWifi,
        Boolean includesHousekeeping,
        Integer sortOrder,
        Boolean isActive
) {}
