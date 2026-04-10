package com.safar.user.repository;

import com.safar.user.entity.PriceAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface PriceAlertRepository extends JpaRepository<PriceAlert, UUID> {
    List<PriceAlert> findByListingIdAndActiveTrue(UUID listingId);
    List<PriceAlert> findByEmailAndActiveTrue(String email);
    List<PriceAlert> findByUserIdAndActiveTrue(UUID userId);
    long countByActiveTrue();
}
