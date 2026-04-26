package com.safar.chef.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Partial-update payload for a DRAFT service listing. All fields nullable;
 * only non-null fields are applied. Service-type cannot be changed (would
 * require deleting and re-creating the listing).
 */
public record UpdateServiceListingRequest(
        String businessName,
        String vendorSlug,
        String heroImageUrl,
        String tagline,
        String aboutMd,
        Integer foundedYear,

        List<String> cities,
        String homeCity,
        String homePincode,
        String homeAddress,
        BigDecimal homeLat,
        BigDecimal homeLng,
        Integer deliveryRadiusKm,
        Boolean outstationCapable,
        List<String> deliveryChannels,

        String pricingPattern,
        String pricingFormula,

        String calendarMode,
        Integer defaultLeadTimeHours,

        String cancellationPolicy,
        String cancellationTermsMd,

        Map<String, Object> typeAttributes
) {}
