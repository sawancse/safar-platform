package com.safar.user.repository;

import com.safar.user.entity.TasteProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TasteProfileRepository extends JpaRepository<TasteProfile, UUID> {
    Optional<TasteProfile> findByUserId(UUID userId);
}
