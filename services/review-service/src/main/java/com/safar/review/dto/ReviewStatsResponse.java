package com.safar.review.dto;

import java.util.UUID;

public record ReviewStatsResponse(
        UUID listingId,
        long totalReviews,
        double averageRating,
        Double avgCleanliness,
        Double avgLocation,
        Double avgValue,
        Double avgCommunication,
        Double avgCheckIn,
        Double avgAccuracy,
        Double avgStaff,
        Double avgFacilities,
        Double avgComfort,
        Double avgFreeWifi
) {
    public ReviewStatsResponse(UUID listingId, long totalReviews, double averageRating) {
        this(listingId, totalReviews, averageRating, null, null, null, null, null, null, null, null, null, null);
    }
}
