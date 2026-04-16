package com.safar.flight.repository;

import com.safar.flight.entity.FlightBooking;
import com.safar.flight.entity.FlightBookingStatus;
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
public interface FlightBookingRepository extends JpaRepository<FlightBooking, UUID> {

    Page<FlightBooking> findByUserId(UUID userId, Pageable pageable);

    Page<FlightBooking> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Optional<FlightBooking> findByBookingRef(String bookingRef);

    Optional<FlightBooking> findByDuffelOrderId(String duffelOrderId);

    List<FlightBooking> findByStatus(FlightBookingStatus status);

    // ── Admin queries ──

    @Query("SELECT fb FROM FlightBooking fb WHERE " +
            "(:status IS NULL OR fb.status = :status) " +
            "AND (:fromDate IS NULL OR fb.departureDate >= :fromDate) " +
            "AND (:toDate IS NULL OR fb.departureDate <= :toDate) " +
            "AND (:origin IS NULL OR fb.departureCityCode = :origin) " +
            "AND (:destination IS NULL OR fb.arrivalCityCode = :destination) " +
            "ORDER BY fb.createdAt DESC")
    Page<FlightBooking> adminSearch(
            @Param("status") FlightBookingStatus status,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("origin") String origin,
            @Param("destination") String destination,
            Pageable pageable);

    long countByStatus(FlightBookingStatus status);

    @Query("SELECT COALESCE(SUM(fb.totalAmountPaise), 0) FROM FlightBooking fb WHERE fb.status IN ('CONFIRMED', 'TICKETED', 'COMPLETED')")
    long totalRevenuePaise();

    @Query("SELECT fb.departureCityCode, fb.arrivalCityCode, COUNT(fb) FROM FlightBooking fb " +
            "WHERE fb.status != 'CANCELLED' GROUP BY fb.departureCityCode, fb.arrivalCityCode ORDER BY COUNT(fb) DESC")
    List<Object[]> topRoutes(Pageable pageable);

    // For notification schedulers — find bookings departing tomorrow
    List<FlightBooking> findByDepartureDateAndStatusIn(LocalDate date, List<FlightBookingStatus> statuses);

    // Pending payment older than 30 min
    @Query("SELECT fb FROM FlightBooking fb WHERE fb.status = 'PENDING_PAYMENT' AND fb.createdAt < :cutoff")
    List<FlightBooking> findStalePendingPayments(@Param("cutoff") java.time.Instant cutoff);
}
