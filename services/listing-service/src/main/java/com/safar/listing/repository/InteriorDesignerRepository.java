package com.safar.listing.repository;

import com.safar.listing.entity.InteriorDesigner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InteriorDesignerRepository extends JpaRepository<InteriorDesigner, UUID> {

    List<InteriorDesigner> findByActiveTrue();

    List<InteriorDesigner> findByCityAndActiveTrue(String city);

    Optional<InteriorDesigner> findByUserId(UUID userId);

    List<InteriorDesigner> findByVerificationStatus(String verificationStatus);

    boolean existsByUserId(UUID userId);
}
