package com.safar.services.dto;

import java.util.List;

public record CreateChefMenuRequest(
        String name,
        String description,
        String serviceType,
        String cuisineType,
        String mealType,
        Long pricePerPlatePaise,
        Integer minGuests,
        Integer maxGuests,
        Boolean isVeg,
        Boolean isVegan,
        Boolean isJain,
        List<MenuItemRequest> items
) {}
