package com.safar.supply.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Internal endpoint payload — chef-service / pg-service / maintenance-service
 * call this after a job is delivered, to debit stock for the BOM.
 */
public record ConsumeStockRequest(
        String refType,    // EVENT_BOOKING / PG_TENANCY / MAINTENANCE_TICKET
        UUID refId,
        List<Item> items
) {
    public record Item(
            String itemKey,
            BigDecimal qty
    ) {}
}
