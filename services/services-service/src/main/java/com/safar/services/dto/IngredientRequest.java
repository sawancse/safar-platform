package com.safar.services.dto;

import java.math.BigDecimal;

public record IngredientRequest(
        String name,
        BigDecimal quantity,
        String unit,
        String category,
        Boolean isOptional,
        String notes
) {}
