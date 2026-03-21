package com.safar.user.repository;

import com.safar.user.entity.MedicalProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface MedicalProfileRepository extends JpaRepository<MedicalProfile, UUID> {
    Optional<MedicalProfile> findByUserId(UUID userId);
    void deleteByUserId(UUID userId);
}
