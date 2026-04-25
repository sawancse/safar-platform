package com.safar.supply.service;

import com.safar.supply.adapter.OrderStatusSnapshot;
import com.safar.supply.adapter.SupplierAdapter;
import com.safar.supply.adapter.SupplierAdapterException;
import com.safar.supply.adapter.SupplierAdapterRegistry;
import com.safar.supply.entity.PurchaseOrder;
import com.safar.supply.entity.Supplier;
import com.safar.supply.entity.enums.IntegrationType;
import com.safar.supply.entity.enums.PurchaseOrderStatus;
import com.safar.supply.repository.PurchaseOrderRepository;
import com.safar.supply.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Polls in-flight POs (ISSUED / ACKNOWLEDGED / IN_TRANSIT) at integrated
 * suppliers every 15 min. Updates external_status + transitions our PO if
 * the supplier's mapped status changed.
 *
 * Skips MANUAL suppliers (admin advances state by hand). Gated on
 * supply.adapters.enabled.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PoStatusPollJob {

    private final PurchaseOrderRepository poRepo;
    private final SupplierRepository supplierRepo;
    private final SupplierAdapterRegistry registry;
    private final PurchaseOrderService poService;

    @Value("${supply.adapters.enabled:false}")
    private boolean adaptersEnabled;

    @Scheduled(fixedRateString = "PT15M")
    public void poll() {
        if (!adaptersEnabled) return;

        List<PurchaseOrder> inFlight = poRepo.findByStatusOrderByCreatedAtDesc(PurchaseOrderStatus.ISSUED);
        inFlight.addAll(poRepo.findByStatusOrderByCreatedAtDesc(PurchaseOrderStatus.ACKNOWLEDGED));
        inFlight.addAll(poRepo.findByStatusOrderByCreatedAtDesc(PurchaseOrderStatus.IN_TRANSIT));

        for (PurchaseOrder po : inFlight) {
            Supplier supplier = supplierRepo.findById(po.getSupplierId()).orElse(null);
            if (supplier == null || supplier.getIntegrationType() == IntegrationType.MANUAL) continue;
            pollOne(po, supplier);
        }
    }

    private void pollOne(PurchaseOrder po, Supplier supplier) {
        try {
            SupplierAdapter adapter = registry.forType(supplier.getIntegrationType());
            OrderStatusSnapshot snap = adapter.fetchStatus(po);
            po.setExternalStatus(snap.externalStatus());
            po.setExternalSyncedAt(OffsetDateTime.now());
            poRepo.save(po);

            if (snap.mappedStatus() != null && snap.mappedStatus() != po.getStatus()) {
                try {
                    poService.transition(po.getId(), snap.mappedStatus());
                } catch (IllegalStateException ignored) {
                    // Invalid transition (e.g. supplier reports IN_TRANSIT but we've
                    // already marked DELIVERED). Don't blow up — admin can reconcile.
                    log.debug("Skipping disallowed transition for {}: {} → {}",
                            po.getPoNumber(), po.getStatus(), snap.mappedStatus());
                }
            }
        } catch (SupplierAdapterException e) {
            log.warn("Status poll failed for {}: {}", po.getPoNumber(), e.getMessage());
            po.setExternalError(e.getMessage());
            poRepo.save(po);
        }
    }
}
