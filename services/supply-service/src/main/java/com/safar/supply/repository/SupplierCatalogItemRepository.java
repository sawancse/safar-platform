package com.safar.supply.repository;

import com.safar.supply.entity.SupplierCatalogItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SupplierCatalogItemRepository extends JpaRepository<SupplierCatalogItem, UUID> {

    List<SupplierCatalogItem> findBySupplierIdOrderByItemLabelAsc(UUID supplierId);

    List<SupplierCatalogItem> findBySupplierIdAndActiveTrueOrderByItemLabelAsc(UUID supplierId);

    Optional<SupplierCatalogItem> findBySupplierIdAndItemKey(UUID supplierId, String itemKey);
}
