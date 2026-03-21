package com.safar.listing.dto;

import java.util.UUID;

public record SafetyScoreDto(
        UUID listingId,
        double overallScore,
        double crimeScore,
        double reviewScore,
        double amenityScore,
        boolean womenFriendly,
        double womenScore,
        String label,
        String summary
) {}
