package com.safar.listing.dto;

import com.safar.listing.entity.enums.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record UpdateSalePropertyRequest(
        String title,
        String description,
        SalePropertyType salePropertyType,
        TransactionType transactionType,
        SellerType sellerType,

        String addressLine1,
        String addressLine2,
        String locality,
        String city,
        String state,
        String pincode,
        BigDecimal lat,
        BigDecimal lng,
        String landmark,

        Long askingPricePaise,
        Boolean priceNegotiable,
        Long maintenancePaise,
        Long bookingAmountPaise,

        Integer carpetAreaSqft,
        Integer builtUpAreaSqft,
        Integer superBuiltUpAreaSqft,
        Integer plotAreaSqft,
        AreaUnit areaUnit,

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

        PossessionStatus possessionStatus,
        LocalDate possessionDate,
        String builderName,
        String projectName,
        String reraId,

        List<String> amenities,
        WaterSupply waterSupply,
        PowerBackup powerBackup,
        Boolean gatedCommunity,
        Boolean cornerProperty,
        Boolean vastuCompliant,
        Boolean petAllowed,
        List<String> overlooking,

        List<String> photos,
        String floorPlanUrl,
        String videoTourUrl,
        String brochureUrl
) {}
