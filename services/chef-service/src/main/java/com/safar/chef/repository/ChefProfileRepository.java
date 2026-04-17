package com.safar.chef.repository;

import com.safar.chef.entity.ChefProfile;
import com.safar.chef.entity.enums.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

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
}
