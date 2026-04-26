package com.safar.services.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Vendor-supplied payload to create a DRAFT service listing.
 *
 * {@code typeAttributes} carries the per-service-type child fields
 * (e.g. flavoursOffered for CAKE_DESIGNER, genres for SINGER). The service
 * layer dispatches via {@code serviceType} to the right child entity class.
 */
public record CreateServiceListingRequest(
        String serviceType,                 // ServiceListingType enum name
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

        String pricingPattern,              // PER_UNIT_TIERED / PER_TIME_BLOCK / FLAT_PER_ITEM / QUOTE_ON_REQUEST
        String pricingFormula,              // JSON string

        String calendarMode,                // DAY_GRAIN / SLOT_GRAIN
        Integer defaultLeadTimeHours,

        String cancellationPolicy,
        String cancellationTermsMd,

        Map<String, Object> typeAttributes  // child-table fields, free-form
) {}
