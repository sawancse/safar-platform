package com.safar.listing.dto;

import java.util.List;
import java.util.UUID;

public record MedicalStayPackageDto(
    UUID id,
    UUID listingId,
    String listingTitle,
    String city,
    String state,
    Long basePricePaise,
    String primaryPhotoUrl,
    List<String> amenities,
    UUID hospitalId,
    String hospitalName,
    String hospitalCity,
    List<String> specialties,
    List<String> accreditations,
    Double hospitalRating,
    Double distanceKm,
    Boolean includesPickup,
    Boolean includesTranslator,
    Boolean caregiverFriendly,
    Long medicalPricePaise,
    Integer minStayNights,
    Integer recoveryDays
) {}
