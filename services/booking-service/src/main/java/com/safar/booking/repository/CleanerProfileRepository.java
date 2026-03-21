package com.safar.booking.repository;

import com.safar.booking.entity.CleanerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface CleanerProfileRepository extends JpaRepository<CleanerProfile, UUID> {

    @Query("SELECT c FROM CleanerProfile c WHERE c.cities LIKE %:city% AND c.available = true AND c.verified = true ORDER BY c.rating DESC NULLS LAST")
    List<CleanerProfile> findAvailableByCity(String city);
}
