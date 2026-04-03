package com.safar.listing.repository;

import com.safar.listing.entity.SalePriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SalePriceHistoryRepository extends JpaRepository<SalePriceHistory, UUID> {

    List<SalePriceHistory> findBySalePropertyIdOrderByChangedAtDesc(UUID salePropertyId);
}
