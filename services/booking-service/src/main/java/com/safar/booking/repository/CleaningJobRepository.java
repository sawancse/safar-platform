package com.safar.booking.repository;

import com.safar.booking.entity.CleaningJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CleaningJobRepository extends JpaRepository<CleaningJob, UUID> {

    List<CleaningJob> findByListingId(UUID listingId);

    Optional<CleaningJob> findByCleanerIdAndId(UUID cleanerId, UUID id);
}
