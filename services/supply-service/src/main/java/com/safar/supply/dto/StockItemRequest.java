package com.safar.supply.dto;

import com.safar.supply.entity.enums.ItemCategory;
import com.safar.supply.entity.enums.ItemUnit;

import java.math.BigDecimal;

public record StockItemRequest(
        String itemKey,
        String itemLabel,
        ItemCategory category,
        ItemUnit unit,
        BigDecimal reorderPoint,
        BigDecimal reorderQty,
        Boolean active
) {}
