package com.safar.listing.repository;

import com.safar.listing.entity.PgPackage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PgPackageRepository extends JpaRepository<PgPackage, UUID> {
    List<PgPackage> findByListingIdAndIsActiveTrueOrderBySortOrder(UUID listingId);
    List<PgPackage> findByListingIdOrderBySortOrder(UUID listingId);
}
