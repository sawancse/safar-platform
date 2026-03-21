package com.safar.listing.repository;

import com.safar.listing.entity.RwCertification;
import com.safar.listing.entity.enums.RwCertStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RwCertificationRepository extends JpaRepository<RwCertification, UUID> {
    Optional<RwCertification> findByListingId(UUID listingId);
    Page<RwCertification> findByStatus(RwCertStatus status, Pageable pageable);
}
