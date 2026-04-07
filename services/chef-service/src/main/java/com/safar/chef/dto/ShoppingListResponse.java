package com.safar.chef.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ShoppingListResponse(
        UUID menuId,
        String menuName,
        int guestCount,
        List<ShoppingCategory> categories
) {

    public record ShoppingCategory(
            String category,
            List<ShoppingItem> items
    ) {}

    public record ShoppingItem(
            String name,
            BigDecimal quantityPerServing,
            BigDecimal totalQuantity,
            String unit,
            boolean isOptional,
            String notes,
            String fromDish
    ) {}
}
