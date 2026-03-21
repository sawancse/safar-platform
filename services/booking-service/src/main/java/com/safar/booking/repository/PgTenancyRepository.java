package com.safar.booking.repository;

import com.safar.booking.entity.PgTenancy;
import com.safar.booking.entity.enums.TenancyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PgTenancyRepository extends JpaRepository<PgTenancy, UUID> {

    Page<PgTenancy> findByListingId(UUID listingId, Pageable pageable);

    Page<PgTenancy> findByTenantId(UUID tenantId, Pageable pageable);

    Page<PgTenancy> findByListingIdAndStatus(UUID listingId, TenancyStatus status, Pageable pageable);

    List<PgTenancy> findByStatusAndNextBillingDate(TenancyStatus status, LocalDate date);

    Optional<PgTenancy> findByTenancyRef(String tenancyRef);

    long countByListingIdAndStatus(UUID listingId, TenancyStatus status);
}
