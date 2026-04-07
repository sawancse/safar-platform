package com.safar.chef.dto;

import java.util.List;

public record MenuItemRequest(
        String name,
        String description,
        String category,
        Boolean isVeg,
        List<IngredientRequest> ingredients
) {}
