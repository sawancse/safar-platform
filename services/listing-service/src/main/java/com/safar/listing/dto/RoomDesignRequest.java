package com.safar.listing.dto;

public record RoomDesignRequest(
        String roomType,
        Integer areaSqft,
        String designStyle
) {}
