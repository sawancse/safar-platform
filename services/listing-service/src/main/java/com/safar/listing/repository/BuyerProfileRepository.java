package com.safar.listing.repository;

import com.safar.listing.entity.BuyerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BuyerProfileRepository extends JpaRepository<BuyerProfile, UUID> {

    Optional<BuyerProfile> findByUserId(UUID userId);

    @Query("SELECT bp FROM BuyerProfile bp WHERE bp.alertsEnabled = true")
    List<BuyerProfile> findAllWithAlertsEnabled();
}
