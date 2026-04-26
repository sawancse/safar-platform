package com.safar.booking.repository;

import com.safar.booking.entity.Trip;
import com.safar.booking.entity.enums.TripStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface TripRepository extends JpaRepository<Trip, UUID> {

    Page<Trip> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    List<Trip> findByUserIdAndStatus(UUID userId, TripStatus status);

    /**
     * Trips departing in the given window — used by the cross-vertical
     * suggestion engine's pre-departure reminder job (T-2 days).
     */
    List<Trip> findByStartDateAndStatus(LocalDate startDate, TripStatus status);
}
