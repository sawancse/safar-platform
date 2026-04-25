package com.safar.supply.adapter;

import com.safar.supply.entity.PurchaseOrder;
import com.safar.supply.entity.PurchaseOrderItem;
import com.safar.supply.entity.SupplierCatalogItem;
import com.safar.supply.entity.enums.IntegrationType;

import java.util.List;
import java.util.UUID;

/**
 * Plug for an external B2B supplier API.
 *
 * Each implementation is a {@code @Component}. The {@link SupplierAdapterRegistry}
 * collects them at startup and looks one up by {@link #integrationType()} when
 * the PO state machine or scheduled jobs need to talk to a supplier.
 *
 * Implementations live in {@code com.safar.supply.adapter.<vendor>} packages.
 *
 * Method calls are synchronous from the request thread (e.g. placePo runs
 * inside the HTTP handler that issued the PO). Long-running operations
 * (catalog sync) run from a {@code @Scheduled} job, never from a request
 * thread.
 */
public interface SupplierAdapter {

    /** Discriminator value matching {@code Supplier.integrationType}. */
    IntegrationType integrationType();

    /**
     * Push a PO to the supplier. Called when PurchaseOrderService transitions
     * to ISSUED for a supplier whose integration_type != MANUAL.
     *
     * @return supplier's order id (persisted to {@code po.externalRef})
     * @throws SupplierAdapterException on API failure (caller decides whether to retry)
     */
    String placePo(PurchaseOrder po, List<PurchaseOrderItem> items) throws SupplierAdapterException;

    /**
     * Fetch latest order status. Called by the @Scheduled poll job for
     * in-flight POs (ISSUED / ACKNOWLEDGED / IN_TRANSIT).
     */
    OrderStatusSnapshot fetchStatus(PurchaseOrder po) throws SupplierAdapterException;

    /**
     * Pull supplier's current catalog. Called daily by the catalog-sync job.
     * Slow — runs from a worker thread, never a request thread.
     *
     * @param supplierId for the adapter to load supplier-specific config (account number etc.)
     */
    List<SupplierCatalogItem> fetchCatalog(UUID supplierId) throws SupplierAdapterException;

    /**
     * Cancel a PO that's not yet shipped. Best-effort; supplier may refuse
     * if already packed. Implementations may swallow benign rejections and
     * just log.
     */
    void cancelPo(PurchaseOrder po) throws SupplierAdapterException;
}
