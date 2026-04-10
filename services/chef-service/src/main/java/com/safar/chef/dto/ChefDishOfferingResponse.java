package com.safar.chef.dto;

import com.safar.chef.entity.enums.DishCategory;

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
