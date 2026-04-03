package com.safar.booking.repository;

import com.safar.booking.entity.SettlementDeduction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SettlementDeductionRepository extends JpaRepository<SettlementDeduction, UUID> {

    List<SettlementDeduction> findBySettlementId(UUID settlementId);
}
