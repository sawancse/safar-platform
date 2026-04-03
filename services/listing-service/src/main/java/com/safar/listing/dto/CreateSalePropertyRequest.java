package com.safar.listing.dto;

import com.safar.listing.entity.enums.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateSalePropertyRequest(
        @NotBlank String title,
        String description,
        @NotNull SalePropertyType salePropertyType,
        TransactionType transactionType,
        SellerType sellerType,
        UUID linkedListingId,

        // Location
        String addressLine1,
        String addressLine2,
        String locality,
        @NotBlank String city,
        @NotBlank String state,
        @NotBlank String pincode,
        BigDecimal lat,
        BigDecimal lng,
        String landmark,

        // Pricing
        @NotNull @Positive Long askingPricePaise,
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
        String brochureUrl
) {}
