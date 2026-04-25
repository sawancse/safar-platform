package com.safar.supply.dto;

import com.safar.supply.entity.enums.ItemCategory;
import com.safar.supply.entity.enums.ItemUnit;

import java.math.BigDecimal;

public record CatalogItemRequest(
        String itemKey,
        String itemLabel,
        ItemCategory category,
        ItemUnit unit,
        Long pricePaise,
        BigDecimal moqQty,
        BigDecimal packSize,
        Integer leadTimeDays,
        String notes,
        Boolean active
) {}
