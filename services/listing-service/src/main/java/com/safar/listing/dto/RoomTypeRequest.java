package com.safar.listing.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record RoomTypeRequest(
        @NotBlank String name,
        String description,
        @NotNull @Min(1) Integer count,
        @NotNull @Min(1) Long basePricePaise,
        @NotNull @Min(1) Integer maxGuests,
        String bedType,
        Integer bedCount,
        Integer areaSqft,
        List<String> amenities,
        // New fields for PG and hourly stay
        String stayMode,       // NIGHTLY, HOURLY
        String sharingType,    // PRIVATE, TWO_SHARING, THREE_SHARING, FOUR_SHARING, DORMITORY
        String roomVariant,    // AC, NON_AC, FURNISHED, SEMI_FURNISHED
        // Photos
        String primaryPhotoUrl,
        List<String> photoUrls // up to 5 URLs
) {}
