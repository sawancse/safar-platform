package com.safar.listing.dto;

import java.util.List;
import java.util.UUID;

public record RoomTypeResponse(
        UUID id,
        UUID listingId,
        String name,
        String description,
        Integer count,
        Long basePricePaise,
        Integer maxGuests,
        String bedType,
        Integer bedCount,
        Integer areaSqft,
        List<String> amenities,
        Integer sortOrder,
        Integer availableCount,
        // PG/Hotel fields
        String stayMode,
        String sharingType,
        String roomVariant,
        Integer totalBeds,
        Integer occupiedBeds,
        // Photos
        String primaryPhotoUrl,
        List<String> photoUrls,
        // Inclusions & Perks
        List<RoomTypeInclusionResponse> inclusions
) {}
