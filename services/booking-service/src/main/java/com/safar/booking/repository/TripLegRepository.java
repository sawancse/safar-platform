package com.safar.booking.repository;

import com.safar.booking.entity.TripLeg;
import com.safar.booking.entity.enums.LegType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TripLegRepository extends JpaRepository<TripLeg, UUID> {

    List<TripLeg> findByTripId(UUID tripId);

    /**
     * Lookup the leg for a given external booking (used when an upstream
     * service notifies us of a status change).
     */
    Optional<TripLeg> findByExternalServiceAndExternalBookingId(
            String externalService, UUID externalBookingId);

    List<TripLeg> findByTripIdAndLegType(UUID tripId, LegType legType);
}
