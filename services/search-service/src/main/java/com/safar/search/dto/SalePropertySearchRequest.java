package com.safar.search.dto;

import java.util.List;

public record SalePropertySearchRequest(
        String query,
        String city,
        String locality,
        List<String> salePropertyType,
        List<String> transactionType,
        List<String> sellerType,
        Long priceMin,
        Long priceMax,
        List<Integer> bedrooms,
        Integer minArea,
        Integer maxArea,
        String possessionStatus,
        String furnishing,
        List<String> facing,
        Integer minFloor,
        Integer maxFloor,
        Integer maxAge,
        Boolean reraVerified,
        Boolean vastuCompliant,
        Boolean gatedCommunity,
        Boolean petAllowed,
        Boolean cornerProperty,
        Boolean verified,
        List<String> amenities,
        Double lat,
        Double lng,
        Double radiusKm,
        String sort,
        Integer page,
        Integer size
) {
    public SalePropertySearchRequest {
        if (page == null || page < 0) page = 0;
        if (size == null || size <= 0) size = 20;
    }
}
