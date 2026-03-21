package com.safar.listing.repository;

import com.safar.listing.entity.Availability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AvailabilityRepository extends JpaRepository<Availability, UUID> {
    List<Availability> findByListingIdAndDateBetween(UUID listingId, LocalDate from, LocalDate to);
    Optional<Availability> findByListingIdAndDate(UUID listingId, LocalDate date);
    List<Availability> findByListingIdAndSourceStartingWith(UUID listingId, String sourcePrefix);
    List<Availability> findByListingIdAndDateBetweenAndSourceStartingWith(UUID listingId, LocalDate from, LocalDate to, String sourcePrefix);
}
