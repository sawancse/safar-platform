package com.safar.listing.repository;

import com.safar.listing.entity.LoanEligibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LoanEligibilityRepository extends JpaRepository<LoanEligibility, UUID> {

    Page<LoanEligibility> findByUserIdOrderByCalculatedAtDesc(UUID userId, Pageable pageable);
}
