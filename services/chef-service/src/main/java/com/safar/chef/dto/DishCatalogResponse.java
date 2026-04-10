package com.safar.chef.dto;

import com.safar.chef.entity.enums.DishCategory;

import java.util.UUID;

public record DishCatalogResponse(
        UUID id,
        String name,
        String description,
        DishCategory category,
        Long pricePaise,
        String photoUrl,
        Boolean isVeg,
        Boolean isRecommended,
        Boolean noOnionGarlic,
        Boolean isFried
) {}
