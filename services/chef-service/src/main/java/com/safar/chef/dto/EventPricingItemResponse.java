package com.safar.chef.dto;

public record EventPricingItemResponse(
        String category,
        String itemKey,
        String label,
        String description,
        String icon,
        Long pricePaise,
        String priceType,
        Long minPricePaise,
        Long maxPricePaise,
        Integer sortOrder,
        Boolean isChefCustom,
        Boolean available
) {}
