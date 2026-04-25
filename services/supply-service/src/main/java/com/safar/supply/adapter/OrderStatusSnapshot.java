package com.safar.supply.adapter;

import com.safar.supply.entity.enums.PurchaseOrderStatus;

import java.time.OffsetDateTime;

/**
 * Latest known status of an in-flight PO at the supplier's side.
 * mappedStatus is what we should transition our PO to (if different).
 */
public record OrderStatusSnapshot(
        String externalStatus,            // supplier's raw status string
        PurchaseOrderStatus mappedStatus, // mapped onto our state machine
        OffsetDateTime estimatedDelivery,
        String trackingUrl
) {}
