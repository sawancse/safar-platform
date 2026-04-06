package com.safar.booking.repository;

import com.safar.booking.entity.InspectionChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InspectionChecklistItemRepository extends JpaRepository<InspectionChecklistItem, UUID> {

    List<InspectionChecklistItem> findBySettlementIdOrderByCreatedAtAsc(UUID settlementId);
}
