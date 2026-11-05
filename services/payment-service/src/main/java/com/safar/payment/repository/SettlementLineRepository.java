package com.safar.payment.repository;

import com.safar.payment.entity.SettlementLine;
import com.safar.payment.entity.enums.RecipientType;
import com.safar.payment.entity.enums.SettlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface SettlementLineRepository extends JpaRepository<SettlementLine, UUID> {
    List<SettlementLine> findByPlanId(UUID planId);
    List<SettlementLine> findByStatus(SettlementStatus status);
    List<SettlementLine> findByRecipientTypeAndStatus(RecipientType recipientType, SettlementStatus status);

    /** Settled HOST lines that are not yet part of any payout. */
    @Query("select l from SettlementLine l where l.recipientType = com.safar.payment.entity.enums.RecipientType.HOST "
            + "and l.status = com.safar.payment.entity.enums.SettlementStatus.COMPLETED and l.recipientId is not null "
            + "and l.amountPaise > 0 and not exists (select 1 from PayoutItem i where i.settlementLineId = l.id)")
    List<SettlementLine> findUnpaidHostLines();
}
