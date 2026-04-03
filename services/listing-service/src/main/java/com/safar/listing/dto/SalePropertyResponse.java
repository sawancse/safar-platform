package com.safar.listing.dto;

import com.safar.listing.entity.enums.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record SalePropertyResponse(
        UUID id,
        UUID sellerId,
        SellerType sellerType,
        UUID linkedListingId,

        String title,
        String description,
        SalePropertyType salePropertyType,
        TransactionType transactionType,

        // Location
        String addressLine1,
        String addressLine2,
        String locality,
        String city,
        String state,
        String pincode,
        BigDecimal lat,
        BigDecimal lng,
        String landmark,

        // Pricing
        Long askingPricePaise,
        Long pricePerSqftPaise,
        Boolean priceNegotiable,
        Long maintenancePaise,
        Long bookingAmountPaise,

        // Area
        Integer carpetAreaSqft,
        Integer builtUpAreaSqft,
        Integer superBuiltUpAreaSqft,
        Integer plotAreaSqft,
        AreaUnit areaUnit,

        // Configuration
        Integer bedrooms,
        Integer bathrooms,
        Integer balconies,
        Integer floorNumber,
        Integer totalFloors,
        FacingDirection facing,
        Integer propertyAgeYears,
        FurnishingStatus furnishing,
        Integer parkingCovered,
        Integer parkingOpen,

        // Construction & Legal
        PossessionStatus possessionStatus,
        LocalDate possessionDate,
        String builderName,
        String projectName,
        String reraId,
        Boolean reraVerified,

        // Features
        List<String> amenities,
        WaterSupply waterSupply,
        PowerBackup powerBackup,
        Boolean gatedCommunity,
        Boolean cornerProperty,
        Boolean vastuCompliant,
        Boolean petAllowed,
        List<String> overlooking,

        // Media
        List<String> photos,
        String floorPlanUrl,
        String videoTourUrl,
        String brochureUrl,

        // Status
        SalePropertyStatus status,
        Boolean featured,
        Boolean verified,
        Integer viewsCount,
        Integer inquiriesCount,
        OffsetDateTime expiresAt,

        // Rental history (from linked listing)
        Double linkedListingAvgRating,
        Integer linkedListingReviewCount,
        Long linkedListingMonthlyRentPaise,

        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime approvedAt
) {}
