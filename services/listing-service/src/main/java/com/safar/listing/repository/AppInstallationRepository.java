package com.safar.listing.repository;

import com.safar.listing.entity.AppInstallation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppInstallationRepository extends JpaRepository<AppInstallation, UUID> {

    Optional<AppInstallation> findByAppIdAndHostId(UUID appId, UUID hostId);

    boolean existsByAppIdAndHostId(UUID appId, UUID hostId);
}
