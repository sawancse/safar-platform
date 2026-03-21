package com.safar.listing.repository;

import com.safar.listing.entity.ManagedStayContract;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ManagedStayContractRepository extends JpaRepository<ManagedStayContract, UUID> {
    List<ManagedStayContract> findByHostId(UUID hostId);
    Optional<ManagedStayContract> findByListingIdAndStatus(UUID listingId, String status);
}
