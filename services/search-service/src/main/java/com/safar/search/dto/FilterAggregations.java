package com.safar.search.dto;

import java.util.Map;

public record FilterAggregations(
        Map<String, Long> types,
        Map<String, Long> amenities,
        Map<String, Long> starRatings,
        Map<String, Long> mealPlans,
        Map<String, Long> cancellationPolicies,
        Map<String, Long> bedTypes,
        Map<String, Long> accessibilityFeatures,
        long petFriendlyCount,
        long instantBookCount,
        long freeCancellationCount,
        long noPrepaymentCount,
        Map<String, Long> priceRanges,
        Map<String, Long> ratingRanges,
        Map<String, Long> bedroomCounts,
        Map<String, Long> bathroomCounts,
        long medicalStayCount,
        Map<String, Long> medicalSpecialties,
        // PG/Hotel aggregations
        Map<String, Long> occupancyTypes,
        Map<String, Long> foodTypes,
        long frontDesk24hCount
) {}
