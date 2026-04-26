package com.safar.services.dto;

import com.safar.services.entity.enums.DishCategory;

import java.util.UUID;

public record ChefDishOfferingResponse(
        UUID dishId,
        String dishName,
        DishCategory category,
        Long catalogPricePaise,
        Long customPricePaise,
        Long effectivePricePaise,
        Boolean isVeg,
        String photoUrl
) {}
