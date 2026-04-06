package com.safar.listing.repository;

import com.safar.listing.entity.LegalCase;
import com.safar.listing.entity.enums.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LegalCaseRepository extends JpaRepository<LegalCase, UUID> {

    Page<LegalCase> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<LegalCase> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<LegalCase> findByStatusOrderByCreatedAtDesc(LegalCaseStatus status, Pageable pageable);

    Page<LegalCase> findByPackageTypeOrderByCreatedAtDesc(LegalPackageType packageType, Pageable pageable);

    Page<LegalCase> findByRiskLevelOrderByCreatedAtDesc(RiskLevel riskLevel, Pageable pageable);

    Page<LegalCase> findByStatusAndPackageTypeOrderByCreatedAtDesc(LegalCaseStatus status, LegalPackageType packageType, Pageable pageable);

    List<LegalCase> findByStatus(LegalCaseStatus status);

    List<LegalCase> findByAdvocateId(UUID advocateId);
}
