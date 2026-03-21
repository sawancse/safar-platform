package com.safar.payment.repository;

import com.safar.payment.entity.SettlementLine;
import com.safar.payment.entity.enums.RecipientType;
import com.safar.payment.entity.enums.SettlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SettlementLineRepository extends JpaRepository<SettlementLine, UUID> {
    List<SettlementLine> findByPlanId(UUID planId);
    List<SettlementLine> findByStatus(SettlementStatus status);
    List<SettlementLine> findByRecipientTypeAndStatus(RecipientType recipientType, SettlementStatus status);
}
