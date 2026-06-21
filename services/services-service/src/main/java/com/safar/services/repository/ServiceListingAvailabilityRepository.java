package com.safar.services.repository;

import com.safar.services.entity.ServiceListingAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServiceListingAvailabilityRepository extends JpaRepository<ServiceListingAvailability, UUID> {

    List<ServiceListingAvailability> findByServiceListingIdAndDateBetweenOrderByDate(
            UUID serviceListingId, LocalDate from, LocalDate to);

    Optional<ServiceListingAvailability> findByServiceListingIdAndDate(UUID serviceListingId, LocalDate date);
}
