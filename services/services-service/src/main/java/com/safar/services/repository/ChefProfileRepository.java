package com.safar.services.repository;

import com.safar.services.entity.ChefProfile;
import com.safar.services.entity.enums.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChefProfileRepository extends JpaRepository<ChefProfile, UUID>,
        JpaSpecificationExecutor<ChefProfile> {

    Optional<ChefProfile> findByUserId(UUID userId);

    Optional<ChefProfile> findByPhone(String phone);

    Optional<ChefProfile> findByEmail(String email);

    List<ChefProfile> findByCity(String city);

    List<ChefProfile> findByVerificationStatus(VerificationStatus verificationStatus);

    List<ChefProfile> findByCityAndAvailableTrue(String city);

    /**
     * Returns [avgRating, count] across all chefs with a non-zero rating.
     * Feeds the /aggregate-ratings landing endpoint.
     */
    @Query("SELECT AVG(c.rating), COUNT(c.id) FROM ChefProfile c WHERE c.rating IS NOT NULL AND c.rating > 0")
    Object[] aggregateChefRating();
}
