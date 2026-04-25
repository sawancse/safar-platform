package com.safar.supply.repository;

import com.safar.supply.entity.PurchaseOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItem, UUID> {

    List<PurchaseOrderItem> findByPoIdOrderByItemLabelAsc(UUID poId);

    void deleteByPoId(UUID poId);
}
