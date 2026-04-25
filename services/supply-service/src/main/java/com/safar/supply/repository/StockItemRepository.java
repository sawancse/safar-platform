package com.safar.supply.repository;

import com.safar.supply.entity.StockItem;
import com.safar.supply.entity.enums.ItemCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StockItemRepository extends JpaRepository<StockItem, UUID> {

    Optional<StockItem> findByItemKey(String itemKey);

    List<StockItem> findByCategoryAndActiveTrueOrderByItemLabelAsc(ItemCategory category);

    List<StockItem> findByActiveTrueOrderByItemLabelAsc();

    @Query("SELECT s FROM StockItem s WHERE s.active = TRUE AND s.reorderPoint IS NOT NULL " +
           "AND s.onHandQty <= s.reorderPoint")
    List<StockItem> findLowStock();
}
