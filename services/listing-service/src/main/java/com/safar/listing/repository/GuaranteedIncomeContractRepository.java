package com.safar.listing.repository;

import com.safar.listing.entity.GuaranteedIncomeContract;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GuaranteedIncomeContractRepository extends JpaRepository<GuaranteedIncomeContract, UUID> {
    List<GuaranteedIncomeContract> findByHostId(UUID hostId);
}
