package com.safar.chef.dto;

import java.util.UUID;

public record MatchedChefResponse(
        UUID chefId,
        String chefName,
        String profilePhotoUrl,
        String city,
        Double rating,
        Integer reviewCount,
        Integer totalBookings,
        Integer experienceYears,
        Integer matchedDishCount,
        Long totalDishCount,
        Long estimatedPricePaise,
        String cuisines,
        Boolean verified,
        String badge
) {}
