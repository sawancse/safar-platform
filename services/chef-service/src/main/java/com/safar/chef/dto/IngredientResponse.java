package com.safar.chef.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record IngredientResponse(
        UUID id,
        UUID menuItemId,
        String name,
        BigDecimal quantity,
        String unit,
        String category,
        Boolean isOptional,
        String notes
) {}
