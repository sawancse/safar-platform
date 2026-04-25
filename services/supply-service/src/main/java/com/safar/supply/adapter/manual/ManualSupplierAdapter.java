package com.safar.supply.adapter.manual;

import com.safar.supply.adapter.OrderStatusSnapshot;
import com.safar.supply.adapter.SupplierAdapter;
import com.safar.supply.entity.PurchaseOrder;
import com.safar.supply.entity.PurchaseOrderItem;
import com.safar.supply.entity.SupplierCatalogItem;
import com.safar.supply.entity.enums.IntegrationType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * No-op adapter for suppliers we coordinate with manually (WhatsApp / phone /
 * email). Returns a synthetic external ref so callers don't need to special-case
 * "no integration"; scheduled jobs explicitly skip MANUAL suppliers.
 *
 * This is the default for every supplier created via the admin UI unless
 * integrationType is explicitly changed.
 */
@Component
public class ManualSupplierAdapter implements SupplierAdapter {

    @Override
    public IntegrationType integrationType() {
        return IntegrationType.MANUAL;
    }

    @Override
    public String placePo(PurchaseOrder po, List<PurchaseOrderItem> items) {
        // Synthetic ref so po.externalRef is never null after issue.
        return "MANUAL-" + po.getPoNumber();
    }

    @Override
    public OrderStatusSnapshot fetchStatus(PurchaseOrder po) {
        // Manual POs are advanced by admin; status doesn't change from outside.
        return new OrderStatusSnapshot(po.getStatus().name(), po.getStatus(), null, null);
    }

    @Override
    public List<SupplierCatalogItem> fetchCatalog(UUID supplierId) {
        // Catalog is admin-managed; no external source to pull from.
        return List.of();
    }

    @Override
    public void cancelPo(PurchaseOrder po) {
        // No external state — nothing to do.
    }
}
