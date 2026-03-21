package com.safar.user.repository;

import com.safar.user.entity.CohostProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface CohostProfileRepository extends JpaRepository<CohostProfile, UUID> {

    Optional<CohostProfile> findByHostId(UUID hostId);

    boolean existsByHostId(UUID hostId);

    @Query("SELECT c FROM CohostProfile c WHERE c.cities LIKE %:city% AND c.active = true AND c.verified = true")
    Page<CohostProfile> findActiveCohostsByCity(String city, Pageable pageable);
}
