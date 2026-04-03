package com.safar.chef.repository;

import com.safar.chef.entity.ChefReferral;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChefReferralRepository extends JpaRepository<ChefReferral, UUID> {
    List<ChefReferral> findByReferrerId(UUID referrerId);
    boolean existsByReferredChefId(UUID referredChefId);
}
