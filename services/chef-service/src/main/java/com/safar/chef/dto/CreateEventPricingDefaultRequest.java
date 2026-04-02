package com.safar.chef.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateEventPricingDefaultRequest(
        @NotBlank String category,
        @NotBlank String itemKey,
        @NotBlank String label,
        String description,
        String icon,
        @NotNull Long defaultPricePaise,
        @NotBlank String priceType,
        Long minPricePaise,
        Long maxPricePaise,
        Integer sortOrder
) {}
