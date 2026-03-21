package com.safar.user.repository;

import com.safar.user.entity.CoTraveler;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CoTravelerRepository extends JpaRepository<CoTraveler, UUID> {
    List<CoTraveler> findByUserIdOrderByCreatedAtDesc(UUID userId);
    long countByUserId(UUID userId);
}
