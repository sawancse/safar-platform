package com.safar.supply.repository;

import com.safar.supply.entity.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StockMovementRepository extends JpaRepository<StockMovement, UUID> {

    List<StockMovement> findByStockItemIdOrderByCreatedAtDesc(UUID stockItemId);

    List<StockMovement> findByRefTypeAndRefIdOrderByCreatedAtDesc(String refType, UUID refId);

    List<StockMovement> findTop100ByOrderByCreatedAtDesc();
}
