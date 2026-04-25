package com.safar.supply.dto;

import com.safar.supply.entity.enums.ItemCategory;
import com.safar.supply.entity.enums.ItemUnit;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreatePurchaseOrderRequest(
        UUID supplierId,
        LocalDate expectedDelivery,
        String deliveryAddress,
        Long taxPaise,           // optional; if null, defaults to 18% of total
        String adminNotes,
        List<LineItem> items,
        Boolean issueImmediately // true → save as ISSUED instead of DRAFT
) {
    public record LineItem(
            UUID catalogItemId,    // optional — if set, label/category/unit/price come from catalog
            String itemKey,
            String itemLabel,
            ItemCategory category,
            ItemUnit unit,
            BigDecimal qty,
            Long unitPricePaise,
            String notes
    ) {}
}
