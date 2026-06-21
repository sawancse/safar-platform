package com.safar.services.dto;

import java.util.List;
import java.util.UUID;

/**
 * A verified pandit listing scored against the customer's criteria, ordered
 * best-first. {@code matchReasons} explains the score for the UI ("Speaks
 * Telugu", "Performs Griha Pravesh", "Brings all samagri").
 */
public record MatchedPanditResponse(
        UUID listingId,
        String businessName,
        String vendorSlug,
        String heroImageUrl,
        String city,
        Double rating,
        Integer reviewCount,
        Integer completedBookings,
        String trustTier,
        String tradition,
        List<String> languages,
        List<String> pujaTypes,
        String samagriProvided,
        Boolean onlineViaVideoCall,
        int matchScore,
        List<String> matchReasons
) {}
