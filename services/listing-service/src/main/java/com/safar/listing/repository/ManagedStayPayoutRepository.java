package com.safar.listing.repository;

import com.safar.listing.entity.ManagedStayPayout;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ManagedStayPayoutRepository extends JpaRepository<ManagedStayPayout, UUID> {
    List<ManagedStayPayout> findByContractId(UUID contractId);
}
