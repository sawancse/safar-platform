package com.safar.supply.service;

import com.safar.supply.adapter.SupplierAdapter;
import com.safar.supply.adapter.SupplierAdapterRegistry;
import com.safar.supply.entity.Supplier;
import com.safar.supply.entity.SupplierCatalogItem;
import com.safar.supply.entity.enums.IntegrationType;
import com.safar.supply.repository.SupplierCatalogItemRepository;
import com.safar.supply.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Daily catalog sync from each integrated supplier. Runs at 03:00 IST.
 * Skips MANUAL suppliers entirely.
 *
 * Gated on supply.adapters.enabled — until that flag flips true,
 * the job logs a one-line skip message and returns.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CatalogSyncJob {

    private final SupplierRepository supplierRepo;
    private final SupplierCatalogItemRepository catalogRepo;
    private final SupplierAdapterRegistry registry;

    @Value("${supply.adapters.enabled:false}")
    private boolean adaptersEnabled;

    /** Daily at 03:00 IST. */
    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Kolkata")
    public void syncAll() {
        if (!adaptersEnabled) {
            log.debug("Catalog sync skipped — supply.adapters.enabled=false");
            return;
        }
        for (Supplier s : supplierRepo.findByActiveTrueOrderByBusinessNameAsc()) {
            if (s.getIntegrationType() == IntegrationType.MANUAL) continue;
            try {
                syncOne(s);
            } catch (Exception e) {
                log.error("Catalog sync failed for {} ({}): {}", s.getBusinessName(),
                        s.getIntegrationType(), e.getMessage());
            }
        }
    }

    @Transactional
    void syncOne(Supplier s) {
        SupplierAdapter adapter = registry.forType(s.getIntegrationType());
        List<SupplierCatalogItem> fresh = adapter.fetchCatalog(s.getId());
        upsert(s.getId(), fresh);
        s.setCatalogSyncedAt(OffsetDateTime.now());
        supplierRepo.save(s);
        log.info("Catalog synced for {} ({} items)", s.getBusinessName(), fresh.size());
    }

    /** Upsert by (supplier_id, item_key); soft-delete missing rows. */
    private void upsert(UUID supplierId, List<SupplierCatalogItem> fresh) {
        List<SupplierCatalogItem> existing = catalogRepo.findBySupplierIdOrderByItemLabelAsc(supplierId);
        Map<String, SupplierCatalogItem> existingByKey = new HashMap<>();
        existing.forEach(c -> existingByKey.put(c.getItemKey(), c));

        Map<String, Boolean> seen = new HashMap<>();
        for (SupplierCatalogItem incoming : fresh) {
            seen.put(incoming.getItemKey(), Boolean.TRUE);
            SupplierCatalogItem cur = existingByKey.get(incoming.getItemKey());
            if (cur == null) {
                incoming.setSupplierId(supplierId);
                catalogRepo.save(incoming);
            } else {
                cur.setItemLabel(incoming.getItemLabel());
                cur.setCategory(incoming.getCategory());
                cur.setUnit(incoming.getUnit());
                cur.setPricePaise(incoming.getPricePaise());
                cur.setMoqQty(incoming.getMoqQty());
                cur.setPackSize(incoming.getPackSize());
                cur.setLeadTimeDays(incoming.getLeadTimeDays());
                cur.setActive(true);
                catalogRepo.save(cur);
            }
        }
        // Soft-delete rows that vanished from supplier's catalog.
        for (SupplierCatalogItem cur : existing) {
            if (!seen.containsKey(cur.getItemKey()) && Boolean.TRUE.equals(cur.getActive())) {
                cur.setActive(false);
                catalogRepo.save(cur);
            }
        }
    }
}
