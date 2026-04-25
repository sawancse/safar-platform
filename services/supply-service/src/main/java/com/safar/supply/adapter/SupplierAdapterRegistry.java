package com.safar.supply.adapter;

import com.safar.supply.entity.Supplier;
import com.safar.supply.entity.enums.IntegrationType;
import com.safar.supply.repository.SupplierRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Spring auto-injects every {@code SupplierAdapter} bean into {@code allAdapters}.
 * On startup we index them by {@link IntegrationType} for O(1) lookup.
 *
 * If a supplier has an integrationType for which no adapter is registered
 * (e.g. METRO_CASH_CARRY before that adapter ships), {@link #forSupplier}
 * throws — defensive, the admin UI prevents picking such types.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SupplierAdapterRegistry {

    private final List<SupplierAdapter> allAdapters;
    private final SupplierRepository supplierRepo;

    private Map<IntegrationType, SupplierAdapter> byType;

    @PostConstruct
    void init() {
        byType = new EnumMap<>(IntegrationType.class);
        for (SupplierAdapter a : allAdapters) {
            IntegrationType t = a.integrationType();
            if (byType.containsKey(t)) {
                throw new IllegalStateException("Two adapters claim type " + t +
                        ": " + byType.get(t).getClass() + " and " + a.getClass());
            }
            byType.put(t, a);
        }
        log.info("Supplier adapters registered: {}", byType.keySet());
    }

    public SupplierAdapter forType(IntegrationType type) {
        SupplierAdapter a = byType.get(type);
        if (a == null) throw new IllegalStateException("No adapter for type " + type);
        return a;
    }

    public SupplierAdapter forSupplier(UUID supplierId) {
        Supplier s = supplierRepo.findById(supplierId)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + supplierId));
        return forType(s.getIntegrationType());
    }

    public Supplier loadSupplier(UUID supplierId) {
        return supplierRepo.findById(supplierId)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + supplierId));
    }
}
