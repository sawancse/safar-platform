package com.safar.supply.repository;

import com.safar.supply.entity.PurchaseOrder;
import com.safar.supply.entity.enums.PurchaseOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, UUID> {

    List<PurchaseOrder> findByOrderByCreatedAtDesc();

    List<PurchaseOrder> findByStatusOrderByCreatedAtDesc(PurchaseOrderStatus status);

    List<PurchaseOrder> findBySupplierIdOrderByCreatedAtDesc(UUID supplierId);

    @Query("SELECT p FROM PurchaseOrder p WHERE p.expectedDelivery < :today " +
           "AND p.status IN (com.safar.supply.entity.enums.PurchaseOrderStatus.ISSUED, " +
                            "com.safar.supply.entity.enums.PurchaseOrderStatus.ACKNOWLEDGED, " +
                            "com.safar.supply.entity.enums.PurchaseOrderStatus.IN_TRANSIT)")
    List<PurchaseOrder> findOverdue(LocalDate today);

    @Query("SELECT COUNT(p) FROM PurchaseOrder p WHERE EXTRACT(MONTH FROM p.createdAt) = EXTRACT(MONTH FROM CURRENT_DATE) " +
           "AND EXTRACT(YEAR FROM p.createdAt) = EXTRACT(YEAR FROM CURRENT_DATE)")
    long countCurrentMonth();
}
