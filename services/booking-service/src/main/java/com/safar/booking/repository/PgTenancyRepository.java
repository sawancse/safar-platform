package com.safar.booking.repository;

import com.safar.booking.entity.PgTenancy;
import com.safar.booking.entity.enums.TenancyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /** Idempotency guard for auto-provisioning: one tenancy per source booking. */
    Optional<PgTenancy> findBySourceBookingId(UUID sourceBookingId);

    long countByListingIdAndStatus(UUID listingId, TenancyStatus status);

    List<PgTenancy> findByStatusAndMoveOutDateLessThanEqual(TenancyStatus status, LocalDate date);

    List<PgTenancy> findByRoomTypeIdAndStatus(UUID roomTypeId, TenancyStatus status);

    @Query("SELECT MAX(t.tenancyRef) FROM PgTenancy t WHERE t.tenancyRef LIKE :prefix")
    Optional<String> findMaxTenancyRefLike(@Param("prefix") String prefix);

    /**
     * Per-room-type count of distinct beds occupied by ACTIVE or NOTICE_PERIOD tenancies.
     * Used by the nightly reconcile job to push the truth back to listing-service's
     * room_types.occupied_beds column, healing accumulated drift from booking +
     * tenancy double-counts.
     */
    @Query("SELECT t.roomTypeId, COUNT(DISTINCT t.bedNumber) "
            + "FROM PgTenancy t "
            + "WHERE t.status IN (com.safar.booking.entity.enums.TenancyStatus.ACTIVE, "
            + "                   com.safar.booking.entity.enums.TenancyStatus.NOTICE_PERIOD) "
            + "  AND t.bedNumber IS NOT NULL "
            + "GROUP BY t.roomTypeId")
    List<Object[]> countOccupiedBedsByRoomType();
}
