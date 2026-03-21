package com.safar.user.repository;

import com.safar.user.entity.SkillSwap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SkillSwapRepository extends JpaRepository<SkillSwap, UUID> {
    Page<SkillSwap> findByCity(String city, Pageable pageable);
}
