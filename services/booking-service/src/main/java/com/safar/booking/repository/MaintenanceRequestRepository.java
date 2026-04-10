package com.safar.booking.repository;

import com.safar.booking.entity.MaintenanceRequest;
import com.safar.booking.entity.enums.MaintenanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface MaintenanceRequestRepository extends JpaRepository<MaintenanceRequest, UUID> {

    Page<MaintenanceRequest> findByTenancyIdOrderByCreatedAtDesc(UUID tenancyId, Pageable pageable);

    Page<MaintenanceRequest> findByTenancyIdAndStatusOrderByCreatedAtDesc(
            UUID tenancyId, MaintenanceStatus status, Pageable pageable);

    long countByTenancyIdAndStatus(UUID tenancyId, MaintenanceStatus status);

    // Booking-linked (hotel/apartment guest service requests)
    Page<MaintenanceRequest> findByBookingIdOrderByCreatedAtDesc(UUID bookingId, Pageable pageable);

    Page<MaintenanceRequest> findByBookingIdAndStatusOrderByCreatedAtDesc(
            UUID bookingId, MaintenanceStatus status, Pageable pageable);

    Page<MaintenanceRequest> findByGuestIdOrderByCreatedAtDesc(UUID guestId, Pageable pageable);

    // Host: all tickets for a listing
    Page<MaintenanceRequest> findByListingIdOrderByCreatedAtDesc(UUID listingId, Pageable pageable);

    Page<MaintenanceRequest> findByListingIdAndStatusOrderByCreatedAtDesc(
            UUID listingId, MaintenanceStatus status, Pageable pageable);

    long countByListingIdAndStatus(UUID listingId, MaintenanceStatus status);

    // SLA breach detection
    List<MaintenanceRequest> findBySlaBreachedFalseAndStatusInAndSlaDeadlineAtBefore(
            List<MaintenanceStatus> statuses, OffsetDateTime now);

    // Admin: all tickets with filters
    Page<MaintenanceRequest> findByStatusInOrderByCreatedAtDesc(
            List<MaintenanceStatus> statuses, Pageable pageable);

    Page<MaintenanceRequest> findBySlaBreachedTrueAndStatusInOrderByCreatedAtDesc(
            List<MaintenanceStatus> statuses, Pageable pageable);

    // Stats
    @Query("SELECT m.category, COUNT(m) FROM MaintenanceRequest m WHERE m.listingId = :lid GROUP BY m.category")
    List<Object[]> countByListingGroupedByCategory(@Param("lid") UUID listingId);

    @Query(value = "SELECT COUNT(*) FROM bookings.maintenance_requests m WHERE m.listing_id = :lid AND m.sla_breached = true AND m.status NOT IN ('CLOSED', 'REJECTED')", nativeQuery = true)
    long countSlaBreachedByListing(@Param("lid") UUID listingId);

    @Query(value = "SELECT AVG(EXTRACT(EPOCH FROM (m.resolved_at - m.created_at)) / 3600) FROM bookings.maintenance_requests m WHERE m.listing_id = :lid AND m.resolved_at IS NOT NULL", nativeQuery = true)
    Double avgResolutionHoursByListing(@Param("lid") UUID listingId);
}
