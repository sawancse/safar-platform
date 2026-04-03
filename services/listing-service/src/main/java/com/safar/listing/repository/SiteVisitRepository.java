package com.safar.listing.repository;

import com.safar.listing.entity.SiteVisit;
import com.safar.listing.entity.enums.VisitStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface SiteVisitRepository extends JpaRepository<SiteVisit, UUID> {

    Page<SiteVisit> findByBuyerIdOrderByScheduledAtDesc(UUID buyerId, Pageable pageable);

    Page<SiteVisit> findBySellerIdOrderByScheduledAtDesc(UUID sellerId, Pageable pageable);

    List<SiteVisit> findBySalePropertyIdAndStatus(UUID salePropertyId, VisitStatus status);

    @Query("SELECT sv FROM SiteVisit sv WHERE sv.sellerId = :sellerId " +
            "AND sv.status = 'CONFIRMED' AND sv.scheduledAt > :now " +
            "ORDER BY sv.scheduledAt ASC")
    List<SiteVisit> findUpcomingVisitsForSeller(@Param("sellerId") UUID sellerId, @Param("now") OffsetDateTime now);

    @Query("SELECT sv FROM SiteVisit sv WHERE sv.buyerId = :buyerId " +
            "AND sv.status IN ('REQUESTED', 'CONFIRMED') AND sv.scheduledAt > :now " +
            "ORDER BY sv.scheduledAt ASC")
    List<SiteVisit> findUpcomingVisitsForBuyer(@Param("buyerId") UUID buyerId, @Param("now") OffsetDateTime now);

    long countBySalePropertyIdAndStatus(UUID salePropertyId, VisitStatus status);
}
