package com.safar.supply.dto;

import java.math.BigDecimal;

public record StockAdjustRequest(
        BigDecimal qtyDelta,    // positive or negative
        String reason,          // ADJUSTMENT_DAMAGE / ADJUSTMENT_COUNT / RETURN
        String notes
) {}
