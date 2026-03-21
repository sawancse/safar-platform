package com.safar.listing.repository;

import com.safar.listing.entity.GuaranteedIncomeSettlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GuaranteedIncomeSettlementRepository extends JpaRepository<GuaranteedIncomeSettlement, UUID> {
    List<GuaranteedIncomeSettlement> findByContractId(UUID contractId);
}
