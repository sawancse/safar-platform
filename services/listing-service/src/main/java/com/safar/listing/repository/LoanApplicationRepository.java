package com.safar.listing.repository;

import com.safar.listing.entity.LoanApplication;
import com.safar.listing.entity.enums.LoanApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LoanApplicationRepository extends JpaRepository<LoanApplication, UUID> {

    Page<LoanApplication> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    List<LoanApplication> findByStatus(LoanApplicationStatus status);
}
