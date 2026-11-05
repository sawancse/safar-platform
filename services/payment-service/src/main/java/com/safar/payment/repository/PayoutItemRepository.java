package com.safar.payment.repository;

import com.safar.payment.entity.PayoutItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PayoutItemRepository extends JpaRepository<PayoutItem, UUID> {
    List<PayoutItem> findByPayoutId(UUID payoutId);
    List<PayoutItem> findBySettlementLineIdIn(Collection<UUID> lineIds);
}
