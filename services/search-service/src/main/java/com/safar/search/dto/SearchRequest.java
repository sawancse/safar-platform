package com.safar.search.dto;

import java.util.List;

public record SearchRequest(
        String query,
        String city,
        List<String> type,
        Long priceMin,
        Long priceMax,
        Double lat,
        Double lng,
        Double radiusKm,
        String sort,
        Boolean instantBook,
        Double minRating,
        Boolean petFriendly,
        Integer minBedrooms,
        Integer minBathrooms,
        List<String> amenities,
        Integer starRating,
        Boolean freeCancellation,
        Boolean noPrepayment,
        String cancellationPolicy,
        String mealPlan,
        List<String> bedTypes,
        List<String> accessibilityFeatures,
        Boolean medicalStay,
        String specialty,
        String procedure,
        Boolean aashrayReady,
        // PG/Hotel filters
        String occupancyType,
        String foodType,
        Boolean frontDesk24h,
        int page,
        int size
) {
    public SearchRequest {
        if (page < 0) page = 0;
        if (size <= 0) size = 20;
    }

    /** Backward-compatible constructor without medical/PG/hotel filters. */
    public SearchRequest(
            String query, String city, List<String> type,
            Long priceMin, Long priceMax,
            Double lat, Double lng, Double radiusKm,
            String sort, Boolean instantBook, Double minRating,
            Boolean petFriendly, Integer minBedrooms, Integer minBathrooms,
            List<String> amenities, Integer starRating,
            Boolean freeCancellation, Boolean noPrepayment,
            String cancellationPolicy, String mealPlan,
            List<String> bedTypes, List<String> accessibilityFeatures,
            int page, int size) {
        this(query, city, type, priceMin, priceMax, lat, lng, radiusKm,
                sort, instantBook, minRating, petFriendly, minBedrooms, minBathrooms,
                amenities, starRating, freeCancellation, noPrepayment,
                cancellationPolicy, mealPlan, bedTypes, accessibilityFeatures,
                null, null, null, null,
                null, null, null, page, size);
    }
}
